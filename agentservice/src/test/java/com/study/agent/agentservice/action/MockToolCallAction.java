package com.study.agent.agentservice.action;

import com.study.agent.agentcore.action.Action;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.ActionResult;
import com.study.agent.agentservice.model.AgentInput;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MockToolCallAction implements Action<AgentInput> {
    @Override
    public Mono<ActionResult> execute(WorkflowContext<AgentInput> ctx) {
        // 模拟工具调用，直接返回成功，不进行真实调用
        return Mono.just(ActionResult.success("mock tool result"));
    }
}
