package com.study.agent.agentcore.store;

import com.study.agent.agentcore.context.WorkflowContext;
import reactor.core.publisher.Mono;

public interface WorkflowStateStore {
    Mono<Void> save(WorkflowContext<?> ctx);

    Mono<WorkflowContext<?>> findByWaitKey(String waitKey);

    Mono<Void> remove(String waitKey);
}