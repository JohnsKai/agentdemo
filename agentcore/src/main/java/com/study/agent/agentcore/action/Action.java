package com.study.agent.agentcore.action;


import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.ActionResult;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface Action<I> {
    Mono<ActionResult> execute(WorkflowContext<I> ctx);
}