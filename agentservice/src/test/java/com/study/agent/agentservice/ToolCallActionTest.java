package com.study.agent.agentservice;


import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.ActionResult;
import com.study.agent.agentcore.model.ActionStatus;
import com.study.agent.agentcore.tool.Tool;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.action.ToolCallAction;
import com.study.agent.agentservice.model.AgentInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ToolCallActionTest {
    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private WorkflowContext<AgentInput> ctx;

    private ToolCallAction action;

    @BeforeEach
    void setUp() {
        action = new ToolCallAction(toolRegistry);
    }

    @Test
    @DisplayName("直接执行工具成功")
    void testDirectSuccess() {
        Map<String, Object> pendingCall = new HashMap<>();
        pendingCall.put("tool", "calculator");
        pendingCall.put("args", Map.of("expr", "1+1"));
        when(ctx.getVariables()).thenReturn(new HashMap<>(Map.of("pending_tool_call", pendingCall)));

        Tool tool = new MockTool("calculator", false, 2);
        when(toolRegistry.getTool("calculator")).thenReturn(tool);

        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.SUCCESS);
                        assertThat(result.getOutput()).isEqualTo(2);
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("需要用户确认的工具返回 ask_user")
    void testRequiresConfirmation() {
        Map<String, Object> pendingCall = new HashMap<>();
        pendingCall.put("tool", "delete_file");
        pendingCall.put("args", Map.of("path", "/tmp/a"));
        when(ctx.getVariables()).thenReturn(new HashMap<>(Map.of("pending_tool_call", pendingCall)));

        Tool tool = new MockTool("delete_file", true, null);
        when(toolRegistry.getTool("delete_file")).thenReturn(tool);

        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.WAITING);
                        assertThat(result.getWaitKey()).startsWith("confirm_delete_file_");
                        Map<String, Object> observations = result.getObservations();
                        assertThat(observations.get("question")).isNotNull();
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("工具不存在返回失败")
    void testToolNotFound() {
        Map<String, Object> pendingCall = new HashMap<>();
        pendingCall.put("tool", "unknown");
        pendingCall.put("args", Map.of());
        when(ctx.getVariables()).thenReturn(new HashMap<>(Map.of("pending_tool_call", pendingCall)));

        when(toolRegistry.getTool("unknown")).thenThrow(new IllegalArgumentException("Tool not found"));

        StepVerifier.create(action.execute(ctx))
                .assertNext(new Consumer<ActionResult>() {
                    @Override
                    public void accept(ActionResult result) {
                        assertThat(result.getStatus()).isEqualTo(ActionStatus.FAILED);
                        assertThat(result.getError()).hasMessageContaining("Tool not found");
                    }
                })
                .verifyComplete();
    }

    // 辅助 MockTool 类
    static class MockTool implements Tool {
        private final String name;
        private final boolean requiresConfirmation;
        private final Object result;

        MockTool(String name, boolean requiresConfirmation, Object result) {
            this.name = name;
            this.requiresConfirmation = requiresConfirmation;
            this.result = result;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "mock";
        }

        @Override
        public Map<String, Object> getParametersSchema() {
            return Map.of();
        }

        @Override
        public boolean requiresConfirmation() {
            return requiresConfirmation;
        }

        @Override
        public Mono<Object> execute(Map<String, Object> args) {
            return Mono.justOrEmpty(result);
        }
    }
}
