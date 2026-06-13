package com.study.agent.agentcore.event;

import com.study.agent.agentcore.model.WorkflowEvent;
import reactor.core.publisher.Mono;

public interface WorkflowEventPublisher {
    Mono<Void> publish(WorkflowEvent event);
}