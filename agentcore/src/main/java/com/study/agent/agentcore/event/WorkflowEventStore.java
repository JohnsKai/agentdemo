package com.study.agent.agentcore.event;

import com.study.agent.agentcore.model.WorkflowEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkflowEventStore {
    Mono<Void> append(WorkflowEvent event);

    Flux<WorkflowEvent> load(String workflowInstanceId);
}