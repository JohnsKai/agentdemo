package com.study.agent.agentservice.store;

import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.store.WorkflowStateStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 仅用于单机测试，后续需要替换持久化存储方案
 */
@Component
public class InMemoryWorkflowStateStore implements WorkflowStateStore {
    private final ConcurrentHashMap<String, WorkflowContext<?>> store = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> save(WorkflowContext<?> ctx) {
        if (ctx.isWaiting() && ctx.getWaitKey() != null) {
            store.put(ctx.getWaitKey(), ctx);
        }
        return Mono.empty();
    }

    @Override
    public Mono<WorkflowContext<?>> findByWaitKey(String waitKey) {
        return Mono.justOrEmpty(store.remove(waitKey));
    }

    @Override
    public Mono<Void> remove(String waitKey) {
        store.remove(waitKey);
        return Mono.empty();
    }
}
