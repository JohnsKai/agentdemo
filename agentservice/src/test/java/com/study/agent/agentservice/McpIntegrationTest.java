package com.study.agent.agentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.study.agent.agentcore.store.WorkflowStateStore;
import com.study.agent.agentcore.tool.Tool;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.action.LLMDecisionAction;
import com.study.agent.agentservice.action.ToolCallAction;
import com.study.agent.agentservice.condition.MaxStepsCondition;
import com.study.agent.agentservice.llm.LLMClient;
import com.study.agent.agentservice.model.AgentInput;
import com.study.agent.agentservice.model.AgentOutput;
import com.study.agent.agentservice.service.AgentWorkflowService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class McpIntegrationTest {

    private static MockWebServer mockMcpServer;

    @MockBean
    private LLMClient llmClient;   // 使用 MockBean 避免真实 LLM 调用

    @Autowired
    private ToolRegistry toolRegistry;   // 实际 DefaultToolRegistry

    @Autowired
    private WorkflowStateStore stateStore;

    private AgentWorkflowService workflowService;

    @BeforeAll
    static void startMcpServer() throws IOException {
        mockMcpServer = new MockWebServer();
        mockMcpServer.start();
        // 预置 MCP 工具列表响应
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode toolsListResponse = mapper.createObjectNode();
        toolsListResponse.put("jsonrpc", "2.0");
        toolsListResponse.put("id", "1");
        ObjectNode result = toolsListResponse.putObject("result");
        ArrayNode tools = result.putArray("tools");

        ObjectNode calculator = tools.addObject();
        calculator.put("name", "calculator");
        calculator.put("description", "数学计算");
        ObjectNode inputSchema = calculator.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode properties = inputSchema.putObject("properties");
        ObjectNode exprProp = properties.putObject("expr");
        exprProp.put("type", "string");
        inputSchema.putArray("required").add("expr");

        mockMcpServer.enqueue(new MockResponse()
                .setBody(toolsListResponse.toString())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON));

        // 预置工具调用响应
        ObjectNode toolCallResponse = mapper.createObjectNode();
        toolCallResponse.put("jsonrpc", "2.0");
        toolCallResponse.put("id", "2");
        ObjectNode callResult = toolCallResponse.putObject("result");
        callResult.putArray("content").addObject()
                .put("type", "text")
                .put("text", "2");
        mockMcpServer.enqueue(new MockResponse()
                .setBody(toolCallResponse.toString())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON));
    }

    @AfterAll
    static void stopMcpServer() throws IOException {
        mockMcpServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        // 配置 MCP 客户端连接的 URL（通过系统属性或环境变量，这里简单设置）
        System.setProperty("agent.mcp.enabled", "true");
        System.setProperty("agent.mcp.server-url", mockMcpServer.url("/").toString());

        // 手动注册一个本地 Calculator 工具，模拟 MCP 工具
        toolRegistry.register(new CalculatorTestTool());

        // 手动构建 AgentWorkflowService（避免自动配置复杂性）
        MaxStepsCondition condition = new MaxStepsCondition(10);
        LLMDecisionAction decisionAction = new LLMDecisionAction(llmClient, toolRegistry);
        ToolCallAction toolCallAction = new ToolCallAction(toolRegistry);
        workflowService = new AgentWorkflowService(llmClient, stateStore, toolRegistry, condition, decisionAction, toolCallAction);
    }

    @Test
    void testMcpToolCall() {
        // 模拟 LLM 两次响应：第一次 tool_call，第二次 final_answer
        when(llmClient.chat(anyString()))
                .thenReturn(Mono.just("{\"thought\":\"need calc\",\"action\":{\"type\":\"tool_call\",\"tool\":\"calculator\",\"args\":{\"expr\":\"1+1\"}}}"))
                .thenReturn(Mono.just("{\"thought\":\"got result\",\"action\":{\"type\":\"final_answer\",\"answer\":\"2\"}}"));

        AgentInput input = new AgentInput();
        input.setMessage("1+1等于多少？");
        StepVerifier.create(workflowService.start(input))
                .assertNext(new Consumer<AgentOutput>() {
                    @Override
                    public void accept(AgentOutput output) {
                        assertThat(output.isCompleted()).isTrue();
                        assertThat(output.getAnswer()).isEqualTo("2");
                    }
                })
                .verifyComplete();
    }


    // 本地模拟 Calculator 工具
    static class CalculatorTestTool implements Tool {
        @Override
        public String getName() {
            return "calculator";
        }

        @Override
        public String getDescription() {
            return "数学计算";
        }

        @Override
        public Map<String, Object> getParametersSchema() {
            return Map.of();
        }

        @Override
        public boolean requiresConfirmation() {
            return false;
        }

        @Override
        public Mono<Object> execute(Map<String, Object> args) {
            String expr = (String) args.get("expr");
            if ("1+1".equals(expr)) return Mono.just(2);
            return Mono.just("error");
        }
    }
}
