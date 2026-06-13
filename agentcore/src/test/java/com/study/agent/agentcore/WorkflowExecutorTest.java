package com.study.agent.agentcore;

import com.study.agent.agentcore.action.Action;
import com.study.agent.agentcore.condition.Condition;
import com.study.agent.agentcore.context.InMemoryWorkflowContext;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.engine.WorkflowExecutor;
import com.study.agent.agentcore.model.ActionResult;
import com.study.agent.agentcore.model.StepResult;
import com.study.agent.agentcore.model.WorkflowId;
import com.study.agent.agentcore.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutorTest {

    private WorkflowId workflowId;
    private WorkflowContext<TestInput> ctx;

    @BeforeEach
    void setUp() {
        workflowId = WorkflowId.generate();
        ctx = new InMemoryWorkflowContext<>(workflowId, new TestInput("hello"));
    }

    @Test
    @DisplayName("一步完成 final_answer")
    void testFinalAnswer() {
        Condition<TestInput> condition = ctx -> Mono.just(true);
        Action<TestInput> decision = ctx -> Mono.just(ActionResult.success(Map.of("type", "final_answer", "answer", "done")));
        Action<TestInput> tool = ctx -> Mono.empty();
        WorkflowExecutor<TestInput> executor = new WorkflowExecutor<>(condition, decision, tool);

        StepResult result = executor.step(ctx).block();
        assertThat(result).isNotNull();
        assertThat(result.getNewStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(result.getFinalOutput()).contains("done");
    }

    @Test
    @DisplayName("工具调用后完成")
    void testToolCallThenFinal() {
        Condition<TestInput> condition = ctx -> Mono.just(true);
        Action<TestInput> decision = new Action<TestInput>() {
            private int count = 0;

            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                if (count == 0) {
                    count++;
                    return Mono.just(ActionResult.success(Map.of("type", "tool_call", "tool", "calc", "args", Map.of("expr", "1+1"))));
                } else {
                    return Mono.just(ActionResult.success(Map.of("type", "final_answer", "answer", "2")));
                }
            }
        };
        Action<TestInput> tool = ctx -> Mono.just(ActionResult.success(2));
        WorkflowExecutor<TestInput> executor = new WorkflowExecutor<>(condition, decision, tool);

        StepResult r1 = executor.step(ctx).block();
        assertThat(r1.getNewStatus()).isEqualTo(WorkflowStatus.RUNNING);
        StepResult r2 = executor.step(ctx).block();
        assertThat(r2.getNewStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(r2.getFinalOutput()).contains("2");
    }

    @Test
    @DisplayName("工具返回等待")
    void testToolWaiting() {
        Condition<TestInput> condition = ctx -> Mono.just(true);
        Action<TestInput> decision = ctx -> Mono.just(ActionResult.success(Map.of("type", "tool_call", "tool", "wait_tool", "args", Map.of())));
        Action<TestInput> tool = ctx -> Mono.just(ActionResult.waiting("test-key"));
        WorkflowExecutor<TestInput> executor = new WorkflowExecutor<>(condition, decision, tool);

        StepResult result = executor.step(ctx).block();
        assertThat(result.getNewStatus()).isEqualTo(WorkflowStatus.WAITING);
        assertThat(result.getWaitKey()).isEqualTo("test-key");
    }

    record TestInput(String msg) {
    }
}