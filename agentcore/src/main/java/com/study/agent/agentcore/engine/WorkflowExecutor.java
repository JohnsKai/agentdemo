package com.study.agent.agentcore.engine;

import com.study.agent.agentcore.action.Action;
import com.study.agent.agentcore.condition.Condition;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.exception.WorkflowException;
import com.study.agent.agentcore.model.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 支持 ReAct 循环（内部递归，支持tool_call、final_answer、ask_user）
 *
 * @param <I>
 */
public class WorkflowExecutor<I> {
    private final Condition<I> condition;
    private final Action<I> decisionAction;
    private final Action<I> toolCallAction;

    public WorkflowExecutor(Condition<I> condition,
                            Action<I> decisionAction,
                            Action<I> toolCallAction) {
        this.condition = condition;
        this.decisionAction = decisionAction;
        this.toolCallAction = toolCallAction;
    }

    /**
     * 执行一步工作流，返回该步产生的事件和新的状态。
     * 注意：该方法不会递归调用自己，只执行一个完整的“思考-行动-观察”周期。
     */
    public Mono<StepResult> step(WorkflowContext<I> ctx) {
        String instanceId = ctx.getWorkflowInstanceId();
        int stepNum = ctx.getCurrentStep() + 1;

        return condition.compare(ctx)
                .flatMap(passed -> {
                    if (!passed) {
                        ctx.setStatus(WorkflowStatus.FAILED);
                        ctx.setError(new WorkflowException("Condition not met"));
                        StepResult result = new StepResult(
                                List.of(WorkflowEvent.failed(instanceId, new WorkflowException("Condition not met"))),
                                WorkflowStatus.FAILED,
                                WorkflowStatus.FAILED);
                        return Mono.just(result);
                    }
                    return decisionAction.execute(ctx)
                            .flatMap(new Function<ActionResult, Mono<? extends StepResult>>() {
                                @Override
                                public Mono<? extends StepResult> apply(ActionResult actionResult) {
                                    return handleDecisionResult(ctx, actionResult);
                                }
                            });
                });
    }

    private Mono<StepResult> handleDecisionResult(WorkflowContext<I> ctx, ActionResult decisionResult) {
        String instanceId = ctx.getWorkflowInstanceId();

        // 处理决策动作失败
        if (decisionResult.getStatus() == ActionStatus.FAILED) {
            ctx.setStatus(WorkflowStatus.FAILED);
            ctx.setError(decisionResult.getError());
            return Mono.just(new StepResult(
                    List.of(WorkflowEvent.failed(instanceId, decisionResult.getError())),
                    WorkflowStatus.FAILED
            ));
        }

        // 处理决策动作直接等待（例如 ask_user）
        if (decisionResult.getStatus() == ActionStatus.WAITING) {
            String waitKey = decisionResult.getWaitKey();
            ctx.setWaiting(true);
            ctx.setWaitKey(waitKey);
            ctx.setStatus(WorkflowStatus.WAITING);
            return Mono.just(new StepResult(
                    List.of(WorkflowEvent.waiting(instanceId, waitKey)),
                    WorkflowStatus.WAITING,
                    null,
                    waitKey
            ));
        }

        // 决策动作成功，解析输出
        Object decisionObj = decisionResult.getOutput();
        if (!(decisionObj instanceof Map)) {
            IllegalStateException ex = new IllegalStateException("Decision output is not a Map");
            ctx.setStatus(WorkflowStatus.FAILED);
            ctx.setError(ex);
            return Mono.just(new StepResult(
                    List.of(WorkflowEvent.failed(instanceId, ex)),
                    WorkflowStatus.FAILED
            ));
        }

        Map<String, Object> decision = (Map<String, Object>) decisionObj;
        String actionType = (String) decision.get("type");

        if ("final_answer".equals(actionType)) {
            String answer = (String) decision.get("answer");
            ctx.setStatus(WorkflowStatus.COMPLETED);
            return Mono.just(new StepResult(
                    List.of(WorkflowEvent.completed(instanceId, answer)),
                    WorkflowStatus.COMPLETED,
                    answer
            ));
        } else if ("tool_call".equals(actionType)) {
            String toolName = (String) decision.get("tool");
            Map<String, Object> args = (Map<String, Object>) decision.get("args");
            // 将待调用信息存入上下文（便于 ToolCallAction 获取）
            ctx.setVariable("pending_tool_call", Map.of("tool", toolName, "args", args));
            // 执行工具调用
            return toolCallAction.execute(ctx)
                    .flatMap(new Function<ActionResult, Mono<StepResult>>() {
                        @Override
                        public Mono<StepResult> apply(ActionResult toolResult) {
                            return handleToolResult(ctx, toolResult, toolName, args);
                        }
                    });
        } else {
            IllegalStateException ex = new IllegalStateException("Unknown action type: " + actionType);
            ctx.setStatus(WorkflowStatus.FAILED);
            ctx.setError(ex);
            return Mono.just(new StepResult(
                    List.of(WorkflowEvent.failed(instanceId, ex)),
                    WorkflowStatus.FAILED
            ));
        }
    }

    /**
     * 处理工具执行结果，转换为 StepResult
     */
    private Mono<StepResult> handleToolResult(WorkflowContext<I> ctx, ActionResult toolResult, String toolName, Object args) {
        String instanceId = ctx.getWorkflowInstanceId();
        if (toolResult.getStatus() == ActionStatus.WAITING) {
            String waitKey = toolResult.getWaitKey();
            ctx.setWaiting(true);
            ctx.setWaitKey(waitKey);
            ctx.setStatus(WorkflowStatus.WAITING);
            return Mono.just(new StepResult(
                    List.of(
                            WorkflowEvent.actionCall(instanceId, toolName, args),
                            WorkflowEvent.waiting(instanceId, waitKey)
                    ),
                    WorkflowStatus.WAITING,
                    null,
                    waitKey   // 传入 waitKey
            ));
        } else if (toolResult.getStatus() == ActionStatus.FAILED) {
            ctx.setStatus(WorkflowStatus.FAILED);
            ctx.setError(toolResult.getError());
            return Mono.just(new StepResult(
                    List.of(
                            WorkflowEvent.actionCall(instanceId, toolName, args),
                            WorkflowEvent.failed(instanceId, toolResult.getError())
                    ),
                    WorkflowStatus.FAILED
            ));
        } else {
            // 工具执行成功
            ctx.setVariable("lastObservation", toolResult.getOutput());
            ctx.incrementStep();
            ctx.touch();
            return Mono.just(new StepResult(
                    List.of(
                            WorkflowEvent.actionCall(instanceId, toolName, args),
                            WorkflowEvent.stepEnd(instanceId, ctx.getCurrentStep())
                    ),
                    WorkflowStatus.RUNNING
            ));
        }
    }


    private String extractType(Object decision) {
        return ((java.util.Map<String, Object>) decision).get("type").toString();
    }

    private String extractAnswer(Object decision) {
        return ((java.util.Map<String, Object>) decision).get("answer").toString();
    }

    private String extractToolName(Object decision) {
        return ((java.util.Map<String, Object>) decision).get("tool").toString();
    }

    private Object extractToolArgs(Object decision) {
        return ((java.util.Map<String, Object>) decision).get("args");
    }
}
