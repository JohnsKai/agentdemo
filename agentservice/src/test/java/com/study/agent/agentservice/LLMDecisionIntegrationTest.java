package com.study.agent.agentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.study.agent.agentservice.llm.LLMClient;
import com.study.agent.agentservice.model.AgentInput;
import com.study.agent.agentservice.model.AgentOutput;
import com.study.agent.agentservice.service.AgentWorkflowService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class LLMDecisionIntegrationTest {
    private static MockWebServer mockMcpServer;

    @MockBean
    private LLMClient llmClient;  // 替换真实 LLM 为 Mock，避免真实调用

    @Autowired
    private AgentWorkflowService workflowService;  // 使用 Spring 自动装配的真实服务

    @DynamicPropertySource
    static void mcpProperties(DynamicPropertyRegistry registry) {
        // 动态配置 MCP 服务器地址
        registry.add("agent.mcp.enabled", () -> true);
        registry.add("agent.mcp.server-url", () -> mockMcpServer.url("/").toString());
    }

    @BeforeAll
    static void startMcpServer() throws IOException {
        mockMcpServer = new MockWebServer();
        mockMcpServer.start();

        // 模拟 MCP Server 的 tools/list 响应
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

        // 模拟工具调用响应
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

    @Test
    void testRealWorkflowWithMcpTool() {
        // 模拟 LLM 的两次响应：第一次要求调用计算器，第二次给出最终答案
        when(llmClient.chat(anyString()))
                .thenReturn(Mono.just("{\"thought\":\"需要计算\",\"action\":{\"type\":\"tool_call\",\"tool\":\"calculator\",\"args\":{\"expr\":\"1+1\"}}}"))
                .thenReturn(Mono.just("{\"thought\":\"得到结果\",\"action\":{\"type\":\"final_answer\",\"answer\":\"2\"}}"));

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
}