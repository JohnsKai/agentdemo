package com.study.agent.agentcore;

import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.ActionResult;
import reactor.core.publisher.Mono;

import java.util.Map;

public class TestActions {
    // 始终返回 final_answer 的动作
    public static <I> Mono<ActionResult> finalAnswerAction(WorkflowContext<I> ctx, String answer) {
        return Mono.just(ActionResult.success(Map.of("type", "final_answer", "answer", answer)));
    }

    // 返回 tool_call 的动作
    public static <I> Mono<ActionResult> toolCallAction(WorkflowContext<I> ctx, String toolName, Map<String, Object> args) {
        return Mono.just(ActionResult.success(Map.of("type", "tool_call", "tool", toolName, "args", args)));
    }

    // 模拟工具执行成功
    public static <I> Mono<ActionResult> successTool(WorkflowContext<I> ctx, Object output) {
        return Mono.just(ActionResult.success(output));
    }

    // 模拟工具执行等待
    public static <I> Mono<ActionResult> waitingTool(WorkflowContext<I> ctx, String waitKey) {
        return Mono.just(ActionResult.waiting(waitKey));
    }

    // 模拟工具执行失败
    public static <I> Mono<ActionResult> failingTool(WorkflowContext<I> ctx, Throwable error) {
        return Mono.just(ActionResult.failed(error));
    }
}
