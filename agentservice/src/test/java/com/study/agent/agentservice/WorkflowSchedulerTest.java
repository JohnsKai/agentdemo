package com.study.agent.agentservice;

import com.study.agent.agentcore.action.Action;
import com.study.agent.agentcore.condition.Condition;
import com.study.agent.agentcore.context.InMemoryWorkflowContext;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.engine.WorkflowExecutor;
import com.study.agent.agentcore.engine.WorkflowScheduler;
import com.study.agent.agentcore.model.ActionResult;
import com.study.agent.agentcore.model.StepResult;
import com.study.agent.agentcore.model.WorkflowId;
import com.study.agent.agentcore.model.WorkflowStatus;
import com.study.agent.agentcore.store.WorkflowStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkflowSchedulerTest {
    private WorkflowId workflowId;
    private WorkflowContext<TestInput> ctx;
    private WorkflowStateStore stateStore;

    @BeforeEach
    void setUp() {
        workflowId = WorkflowId.generate();
        ctx = new InMemoryWorkflowContext<>(workflowId, new TestInput("hello"));
        stateStore = new InMemoryStateStore();
    }

    // 辅助方法：创建一步完成的工作流执行器
    private WorkflowExecutor<TestInput> createFinalAnswerExecutor(final String answer) {
        Condition<TestInput> condition = new Condition<TestInput>() {
            @Override
            public Mono<Boolean> compare(WorkflowContext<TestInput> ctx) {
                return Mono.just(true);
            }
        };
        Action<TestInput> decision = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.just(ActionResult.success(Map.of("type", "final_answer", "answer", answer)));
            }
        };
        Action<TestInput> tool = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.empty(); // 不会被调用
            }
        };
        return new WorkflowExecutor<>(condition, decision, tool);
    }

    // 辅助方法：创建需要工具调用后完成的工作流执行器
    private WorkflowExecutor<TestInput> createToolCallExecutor(final String toolName, final Map<String, Object> args,
                                                               final Object toolResult, final String finalAnswer) {
        Condition<TestInput> condition = new Condition<TestInput>() {
            @Override
            public Mono<Boolean> compare(WorkflowContext<TestInput> ctx) {
                return Mono.just(true);
            }
        };
        // 决策：第一次返回 tool_call，第二次返回 final_answer
        Action<TestInput> decision = new Action<TestInput>() {
            private int callCount = 0;

            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                if (callCount == 0) {
                    callCount++;
                    return Mono.just(ActionResult.success(Map.of("type", "tool_call", "tool", toolName, "args", args)));
                } else {
                    return Mono.just(ActionResult.success(Map.of("type", "final_answer", "answer", finalAnswer)));
                }
            }
        };
        Action<TestInput> tool = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.just(ActionResult.success(toolResult));
            }
        };
        return new WorkflowExecutor<>(condition, decision, tool);
    }

    // 辅助方法：创建返回 WAITING 的工具调用执行器
    private WorkflowExecutor<TestInput> createWaitingExecutor(final String waitKey) {
        Condition<TestInput> condition = new Condition<TestInput>() {
            @Override
            public Mono<Boolean> compare(WorkflowContext<TestInput> ctx) {
                return Mono.just(true);
            }
        };
        Action<TestInput> decision = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.just(ActionResult.success(Map.of("type", "tool_call", "tool", "wait_tool", "args", Map.of())));
            }
        };
        Action<TestInput> tool = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.just(ActionResult.waiting(waitKey));
            }
        };
        return new WorkflowExecutor<>(condition, decision, tool);
    }

    // 辅助方法：创建立即失败的执行器
    private WorkflowExecutor<TestInput> createFailureExecutor() {
        Condition<TestInput> condition = new Condition<TestInput>() {
            @Override
            public Mono<Boolean> compare(WorkflowContext<TestInput> ctx) {
                return Mono.just(true);
            }
        };
        Action<TestInput> decision = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.just(ActionResult.failed(new RuntimeException("LLM error")));
            }
        };
        Action<TestInput> tool = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.empty();
            }
        };
        return new WorkflowExecutor<>(condition, decision, tool);
    }

    @Test
    @DisplayName("正常完成：一步返回 final_answer")
    void testRunUntilStopDirectComplete() {
        WorkflowExecutor<TestInput> executor = createFinalAnswerExecutor("done");
        WorkflowScheduler<TestInput> scheduler = new WorkflowScheduler<>(executor, stateStore);

        StepVerifier.create(scheduler.runUntilStop(ctx))
                .expectNext("done")
                .verifyComplete();

        assertThat(ctx.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
    }

    @Test
    @DisplayName("正常完成：工具调用后返回 final_answer")
    void testRunUntilStopToolCallThenComplete() {
        WorkflowExecutor<TestInput> executor = createToolCallExecutor("calculator", Map.of("expr", "1+1"), 2, "2");
        WorkflowScheduler<TestInput> scheduler = new WorkflowScheduler<>(executor, stateStore);

        StepVerifier.create(scheduler.runUntilStop(ctx))
                .expectNext("2")
                .verifyComplete();

        assertThat(ctx.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(ctx.getCurrentStep()).isEqualTo(1);
    }

    @Test
    @DisplayName("工具返回 WAITING，工作流挂起")
    void testDecisionReturnsWaiting() {
        final String waitKey = "decision-wait-key";
        Condition<TestInput> condition = new Condition<TestInput>() {
            @Override
            public Mono<Boolean> compare(WorkflowContext<TestInput> ctx) {
                return Mono.just(true);
            }
        };
        Action<TestInput> decision = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.just(ActionResult.waiting(waitKey));
            }
        };
        // 工具动作不会被执行，可以传空实现
        Action<TestInput> tool = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.empty();
            }
        };
        WorkflowExecutor<TestInput> executor = new WorkflowExecutor<>(condition, decision, tool);
        StepResult result = executor.step(ctx).block();
        assertThat(result.getNewStatus()).isEqualTo(WorkflowStatus.WAITING);
        assertThat(result.getWaitKey()).hasValue(waitKey);
    }

    @Test
    @DisplayName("从等待中恢复并完成")
    void testResumeAfterWaiting() {
        final String waitKey = "test-wait-key";

        // 条件始终通过
        Condition<TestInput> condition = new Condition<TestInput>() {
            @Override
            public Mono<Boolean> compare(WorkflowContext<TestInput> ctx) {
                return Mono.just(true);
            }
        };

        // 决策：第一次返回 tool_call，第二次返回 final_answer
        Action<TestInput> decision = new Action<TestInput>() {
            private int callCount = 0;

            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                if (callCount == 0) {
                    callCount++;
                    return Mono.just(ActionResult.success(Map.of(
                            "type", "tool_call",
                            "tool", "wait_tool",
                            "args", Map.of()
                    )));
                } else {
                    return Mono.just(ActionResult.success(Map.of(
                            "type", "final_answer",
                            "answer", "2"
                    )));
                }
            }
        };

        // 工具：第一次返回 WAITING，第二次不应被调用
        Action<TestInput> tool = new Action<TestInput>() {
            @Override
            public Mono<ActionResult> execute(WorkflowContext<TestInput> ctx) {
                return Mono.just(ActionResult.waiting(waitKey));
            }
        };

        WorkflowExecutor<TestInput> executor = new WorkflowExecutor<>(condition, decision, tool);

        // 第一步：执行 step，应进入 WAITING 状态
        StepResult step1 = executor.step(ctx).block();
        assertThat(step1).isNotNull();
        assertThat(step1.getNewStatus()).isEqualTo(WorkflowStatus.WAITING);
        assertThat(step1.getWaitKey()).hasValue(waitKey);
        assertThat(ctx.getStatus()).isEqualTo(WorkflowStatus.WAITING);

        // 模拟恢复：清除等待标志（如果需要额外数据如用户输入，可以添加到历史，但本例决策不依赖）
        ctx.setWaiting(false);
        ctx.setWaitKey(null);

        // 第二步：再次执行 step，应得到最终答案
        StepResult step2 = executor.step(ctx).block();
        assertThat(step2).isNotNull();
        assertThat(step2.getNewStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(step2.getFinalOutput()).contains("2");
        assertThat(ctx.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
    }

    @Test
    @DisplayName("工作流失败")
    void testRunUntilStopFailure() {
        WorkflowExecutor<TestInput> executor = createFailureExecutor();
        WorkflowScheduler<TestInput> scheduler = new WorkflowScheduler<>(executor, stateStore);

        StepVerifier.create(scheduler.runUntilStop(ctx))
                .expectErrorMatches(throwable -> throwable.getMessage().equals("LLM error"))
                .verify();

        assertThat(ctx.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(ctx.getError()).hasMessage("LLM error");
    }

    // 测试输入类型
    record TestInput(String message) {
    }

    // 内存状态存储实现（仅用于测试）
    static class InMemoryStateStore implements WorkflowStateStore {
        final ConcurrentHashMap<String, WorkflowContext<?>> map = new ConcurrentHashMap<>();

        @Override
        public Mono<Void> save(WorkflowContext<?> ctx) {
            if (ctx.isWaiting() && ctx.getWaitKey() != null) {
                map.put(ctx.getWaitKey(), ctx);
            }
            return Mono.empty();
        }

        @Override
        public Mono<WorkflowContext<?>> findByWaitKey(String waitKey) {
            return Mono.justOrEmpty(map.remove(waitKey));
        }

        @Override
        public Mono<Void> remove(String waitKey) {
            map.remove(waitKey);
            return Mono.empty();
        }
    }
}
