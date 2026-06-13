package com.study.agent.agentservice.action;

import com.study.agent.agentcore.action.Action;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.ActionResult;
import com.study.agent.agentcore.tool.Tool;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.model.AgentInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class ToolCallAction implements Action<AgentInput> {

    private static final Logger log = LoggerFactory.getLogger(ToolCallAction.class);
    private final ToolRegistry toolRegistry;


    private boolean returnWaiting = false;
    private String waitKey = "mock-wait-key";
    private boolean returnFailure = false;
    private Throwable failureError = new RuntimeException("Mock tool failure");

    public ToolCallAction(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public Mono<ActionResult> execute(WorkflowContext<AgentInput> ctx) {
        // 从上下文中获取上一步 LLM 决策中保存的 tool_call 信息
        // 注意：实际调用时，决策信息应通过某种方式传递给本 Action。常见做法是在 WorkflowExecutor 中
        // 先调用 LLMDecisionAction 得到决策，然后将 tool_name 和 args 作为参数传入 ToolCallAction。
        // 但为了解耦，我们约定：在执行 ToolCallAction 之前，WorkflowExecutor 已经将待调用的
        // 工具信息放入了上下文的变量中，例如 ctx.getVariable("pending_tool_call")。
        // 此处为了简化实现，我们直接从上下文的变量中提取信息。
        Map<String, Object> pendingCall = (Map<String, Object>) ctx.getVariables().get("pending_tool_call");
        if (pendingCall == null) {
            return Mono.just(ActionResult.failed(new IllegalStateException("No pending tool call")));
        }

        String toolName = (String) pendingCall.get("tool");
        Map<String, Object> args = (Map<String, Object>) pendingCall.get("args");


        try {
            Tool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                return Mono.just(ActionResult.failed(new IllegalArgumentException("Tool not found: " + toolName)));
            }
            // 检查是否需要用户确认
            if (tool.requiresConfirmation()) {
                String waitKey = "confirm_" + toolName + "_" + UUID.randomUUID();
                // 将待执行的信息保存回上下文，以便恢复时执行
                ctx.setVariable("pending_tool_call", pendingCall);
                ctx.setVariable("waiting_for_confirmation", true);
                String question = "是否确认执行工具 " + toolName + " 参数：" + args;
                return Mono.just(ActionResult.askUser(question, waitKey));
            }

            // 直接执行工具
            return tool.execute(args)
                    .map(new Function<Object, ActionResult>() {
                        @Override
                        public ActionResult apply(Object result) {
                            log.info("Tool {} executed with result: {}", toolName, result);
                            // 清除上下文中待执行的调用信息
                            ctx.removeVariable("pending_tool_call");
                            return ActionResult.success(result);
                        }
                    })
                    .onErrorResume(new Function<Throwable, Mono<ActionResult>>() {
                        @Override
                        public Mono<ActionResult> apply(Throwable e) {
                            log.error("Tool execution failed: {}", toolName, e);
                            return Mono.just(ActionResult.failed(e));
                        }
                    });
        } catch (IllegalArgumentException e) {
            // 工具不存在，返回失败
            return Mono.just(ActionResult.failed(new IllegalArgumentException("Tool not found: " + toolName, e)));
        }

    }
}
