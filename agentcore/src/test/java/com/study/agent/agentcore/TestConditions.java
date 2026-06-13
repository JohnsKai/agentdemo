package com.study.agent.agentcore;

import com.study.agent.agentcore.context.WorkflowContext;
import reactor.core.publisher.Mono;

public class TestConditions {
    public static <I, O> Mono<Boolean> maxStepsCondition(WorkflowContext<I> ctx, int limit) {
        return Mono.just(ctx.getCurrentStep() < limit);
    }
}
