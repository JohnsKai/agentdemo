package com.study.agent.agentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.ActionResult;
import com.study.agent.agentcore.model.ActionStatus;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.action.LLMDecisionAction;
import com.study.agent.agentservice.llm.LLMClient;
import com.study.agent.agentservice.model.AgentInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LLMDecisionActionTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private LLMClient llmClient;
    @Mock
    private ToolRegistry toolRegistry;
    @Mock
    private WorkflowContext<AgentInput> ctx;
    private LLMDecisionAction action;

    @BeforeEach
    void setUp() {
        action = new LLMDecisionAction(llmClient, toolRegistry);
        when(ctx.getConversationHistory()).thenReturn(new ArrayList<>());
    }

    @Test
    @DisplayName("返回 final_answer")
    void testFinalAnswer() {
        String response = "{\"thought\":\"ok\",\"action\":{\"type\":\"final_answer\",\"answer\":\"42\"}}";
        when(llmClient.chat(anyString())).thenReturn(Mono.just(response));
        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.SUCCESS);
                        Map<String, Object> output = (Map<String, Object>) result.getOutput();
                        assertThat(output.get("type")).isEqualTo("final_answer");
                        assertThat(output.get("answer")).isEqualTo("42");
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("返回 tool_call")
    void testToolCall() {
        String response = "{\"thought\":\"calc\",\"action\":{\"type\":\"tool_call\",\"tool\":\"calculator\",\"args\":{\"expr\":\"1+1\"}}}";
        when(llmClient.chat(anyString())).thenReturn(Mono.just(response));
        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.SUCCESS);
                        Map<String, Object> output = (Map<String, Object>) result.getOutput();
                        assertThat(output.get("type")).isEqualTo("tool_call");
                        assertThat(output.get("tool")).isEqualTo("calculator");
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("返回 ask_user")
    void testAskUser() {
        String response = "{\"thought\":\"ask\",\"action\":{\"type\":\"ask_user\",\"question\":\"确认？\",\"waitKey\":\"w1\"}}";
        when(llmClient.chat(anyString())).thenReturn(Mono.just(response));
        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.WAITING);
                        assertThat(result.getWaitKey()).isEqualTo("w1");
                        Map<String, Object> details = result.getObservations();
                        assertThat(details.get("question")).isEqualTo("确认？");
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("缺少 type 字段")
    void testMissingType() {
        String response = "{\"thought\":\"err\",\"action\":{\"invalid\":true}}";
        when(llmClient.chat(anyString())).thenReturn(Mono.just(response));
        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.FAILED);
                        assertThat(result.getError()).hasMessageContaining("Unknown action type");
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("JSON 解析失败")
    void testInvalidJson() {
        when(llmClient.chat(anyString())).thenReturn(Mono.just("not json"));
        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.FAILED);
                        assertThat(result.getError()).isInstanceOf(RuntimeException.class);
                    }
                })
                .verifyComplete();
    }
}