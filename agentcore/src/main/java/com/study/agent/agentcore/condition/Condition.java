package com.study.agent.agentcore.condition;

import com.study.agent.agentcore.context.WorkflowContext;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface Condition<I> {
    Mono<Boolean> compare(WorkflowContext<I> ctx);
}