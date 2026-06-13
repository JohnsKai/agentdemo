package com.study.agent.agentcore;

import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.store.WorkflowStateStore;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * mock workflowstate的持久化实现
 */
public class InMemoryWorkflowStateStore implements WorkflowStateStore {
    private final ConcurrentHashMap<String, WorkflowContext<?>> waitingMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> save(WorkflowContext<?> ctx) {
        if (ctx.isWaiting() && ctx.getWaitKey() != null) {
            waitingMap.put(ctx.getWaitKey(), ctx);
        }
        return Mono.empty();
    }

    @Override
    public Mono<WorkflowContext<?>> findByWaitKey(String waitKey) {
        return Mono.justOrEmpty(waitingMap.remove(waitKey));
    }

    @Override
    public Mono<Void> remove(String waitKey) {
        waitingMap.remove(waitKey);
        return Mono.empty();
    }

    // 供测试验证或清理使用
    public int size() {
        return waitingMap.size();
    }

    public void clear() {
        waitingMap.clear();
    }
}
