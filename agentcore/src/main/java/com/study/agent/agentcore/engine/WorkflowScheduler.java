package com.study.agent.agentcore.engine;


import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.Message;
import com.study.agent.agentcore.model.StepResult;
import com.study.agent.agentcore.model.WorkflowStatus;
import com.study.agent.agentcore.store.WorkflowStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 支持运行直到完成或等待，支持从 WorkflowStateStore 恢复
 *
 * @param <I>
 */
public class WorkflowScheduler<I> {
    private static final Logger log = LoggerFactory.getLogger(WorkflowScheduler.class);
    private final WorkflowExecutor<I> executor;
    private final WorkflowStateStore stateStore;

    public WorkflowScheduler(WorkflowExecutor<I> executor, WorkflowStateStore stateStore) {
        this.executor = executor;
        this.stateStore = stateStore;
    }

    /**
     * 启动工作流，直到完成、失败或进入等待状态。
     * 完成后返回最终输出（Object），失败则发出错误信号，等待则不返回（挂起）。
     */
    public Mono<Object> runUntilStop(WorkflowContext<I> ctx) {
        return stepAndContinue(ctx);
    }

    private Mono<Object> stepAndContinue(WorkflowContext<I> ctx) {
        if (executor == null) {
            return Mono.error(new IllegalStateException("executor is null"));
        }
        Mono<StepResult> stepMono = executor.step(ctx);
        if (stepMono == null) {
            return Mono.error(new IllegalStateException("executor.step(ctx) returned null"));
        }
        return executor.step(ctx)
                .flatMap(new Function<StepResult, Mono<Object>>() {

                    @Override
                    public Mono<Object> apply(StepResult result) {
                        // 处理事件（持久化/日志）
                        result.getEvents().forEach(event -> log.info("Event: {}", event));
                        WorkflowStatus status = result.getNewStatus();
                        if (status == WorkflowStatus.COMPLETED) {
                            return result.getFinalOutput().map(new Function<Object, Mono<Object>>() {
                                        @Override
                                        public Mono<Object> apply(Object output) {
                                            return Mono.just(output);
                                        }
                                    })
                                    .orElseGet(new java.util.function.Supplier<Mono<Object>>() {
                                        @Override
                                        public Mono<Object> get() {
                                            return Mono.error(new IllegalStateException("Missing final output"));
                                        }
                                    });
                        }

                        if (status == WorkflowStatus.FAILED) {
                            Throwable error = ctx.getError();
                            if (error == null) {
                                error = new IllegalStateException("Workflow failed without error");
                            }
                            return Mono.error(error);
                        }
                        if (status == WorkflowStatus.WAITING) {
                            return stateStore.save(ctx).then(Mono.never());
                        }
                        return stepAndContinue(ctx);
                    }
                });
    }

    /**
     * 恢复一个处于 WAITING 状态的工作流。
     */
    public Mono<Object> resume(WorkflowContext<I> ctx, Object resumeEvent) {
        // 将恢复事件追加到对话历史
        ctx.addMessage(new Message("user", resumeEvent.toString()));
        ctx.setWaiting(false);
        ctx.setWaitKey(null);
        return stepAndContinue(ctx);
    }
}
