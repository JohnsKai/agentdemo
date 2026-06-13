package com.study.agent.agentservice.condition;

import com.study.agent.agentcore.condition.Condition;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentservice.model.AgentInput;
import reactor.core.publisher.Mono;

public class MaxStepsCondition implements Condition<AgentInput> {
    private final int maxSteps;

    public MaxStepsCondition(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    @Override
    public Mono<Boolean> compare(WorkflowContext<AgentInput> ctx) {
        return Mono.just(ctx.getCurrentStep() < maxSteps);
    }
}
