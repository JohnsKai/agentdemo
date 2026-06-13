package com.study.agent.agentservice.service;

import com.study.agent.agentcore.action.Action;
import com.study.agent.agentcore.condition.Condition;
import com.study.agent.agentcore.context.InMemoryWorkflowContext;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.engine.WorkflowExecutor;
import com.study.agent.agentcore.engine.WorkflowScheduler;
import com.study.agent.agentcore.model.WorkflowId;
import com.study.agent.agentcore.model.WorkflowStatus;
import com.study.agent.agentcore.store.WorkflowStateStore;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.action.LLMDecisionAction;
import com.study.agent.agentservice.action.ToolCallAction;
import com.study.agent.agentservice.condition.MaxStepsCondition;
import com.study.agent.agentservice.llm.LLMClient;
import com.study.agent.agentservice.model.AgentInput;
import com.study.agent.agentservice.model.AgentOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;


@Service
public class AgentWorkflowService {
    private final static Logger log = LoggerFactory.getLogger(AgentWorkflowService.class);
    private final LLMClient llmClient;
    private final WorkflowStateStore stateStore;
    private final ToolRegistry toolRegistry;
    private final Condition<AgentInput> condition;
    private final Action<AgentInput> decisionAction;
    private final Action<AgentInput> toolCallAction;

    /**
     * 构造器，所有依赖通过 Spring 注入。
     *
     * @param llmClient    LLM 客户端（实际实现由配置决定）
     * @param stateStore   工作流状态存储（如 Redis 或内存实现）
     * @param toolRegistry 工具注册表
     * @param maxSteps     最大步数，从配置读取
     */
    @Autowired
    public AgentWorkflowService(LLMClient llmClient,
                                WorkflowStateStore stateStore,
                                ToolRegistry toolRegistry,
                                @Value("${agent.max-steps:10}") int maxSteps) {
        this.llmClient = llmClient;
        this.stateStore = stateStore;
        this.toolRegistry = toolRegistry;
        this.condition = new MaxStepsCondition(maxSteps);
        this.decisionAction = new LLMDecisionAction(llmClient, toolRegistry);
        this.toolCallAction = new ToolCallAction(toolRegistry);
    }

    /**
     * 全参数构造器（用于测试或自定义行为）。
     */
    public AgentWorkflowService(LLMClient llmClient,
                                WorkflowStateStore stateStore,
                                ToolRegistry toolRegistry,
                                Condition<AgentInput> condition,
                                Action<AgentInput> decisionAction,
                                Action<AgentInput> toolCallAction) {
        this.llmClient = llmClient;
        this.stateStore = stateStore;
        this.toolRegistry = toolRegistry;
        this.condition = condition;
        this.decisionAction = decisionAction;
        this.toolCallAction = toolCallAction;
    }

    /**
     * 启动新的工作流会话。
     */
    public Mono<AgentOutput> start(AgentInput input) {
        String sessionId = input.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        WorkflowId workflowId = WorkflowId.of(sessionId);
        WorkflowContext<AgentInput> ctx = new InMemoryWorkflowContext<>(workflowId, input);

        WorkflowExecutor<AgentInput> executor = new WorkflowExecutor<>(condition, decisionAction, toolCallAction);
        WorkflowScheduler<AgentInput> scheduler = new WorkflowScheduler<>(executor, stateStore);

        return scheduler.runUntilStop(ctx)
                .map(new Function<Object, AgentOutput>() {
                    @Override
                    public AgentOutput apply(Object finalAnswer) {
                        return buildOutput(ctx, finalAnswer);
                    }
                })
                .onErrorResume(new Function<Throwable, Mono<AgentOutput>>() {
                    @Override
                    public Mono<AgentOutput> apply(Throwable e) {
                        log.error("Workflow failed", e);
                        return Mono.just(buildErrorOutput(ctx, e));
                    }
                });
    }

    /**
     * 恢复等待中的工作流。
     *
     * @param waitKey   等待键
     * @param userInput 用户输入（恢复事件）
     */
    public Mono<AgentOutput> resume(String waitKey, Object userInput) {
        return stateStore.findByWaitKey(waitKey)
                .flatMap(new Function<WorkflowContext<?>, Mono<AgentOutput>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public Mono<AgentOutput> apply(WorkflowContext<?> ctx) {
                        WorkflowContext<AgentInput> typedCtx = (WorkflowContext<AgentInput>) ctx;
                        WorkflowExecutor<AgentInput> executor = new WorkflowExecutor<>(condition, decisionAction, toolCallAction);
                        WorkflowScheduler<AgentInput> scheduler = new WorkflowScheduler<>(executor, stateStore);
                        return scheduler.resume(typedCtx, userInput)
                                .map(new Function<Object, AgentOutput>() {
                                    @Override
                                    public AgentOutput apply(Object finalAnswer) {
                                        return buildOutput(typedCtx, finalAnswer);
                                    }
                                });
                    }
                })
                .onErrorResume(new Function<Throwable, Mono<AgentOutput>>() {
                    @Override
                    public Mono<AgentOutput> apply(Throwable e) {
                        log.error("Resume failed", e);
                        AgentOutput errorOutput = new AgentOutput();
                        errorOutput.setCompleted(true);
                        errorOutput.setAnswer("Resume error: " + e.getMessage());
                        return Mono.just(errorOutput);
                    }
                });
    }

    private AgentOutput buildOutput(WorkflowContext<AgentInput> ctx, Object finalAnswer) {
        AgentOutput output = new AgentOutput();
        output.setSessionId(ctx.getWorkflowInstanceId());
        output.setCompleted(ctx.getStatus() == WorkflowStatus.COMPLETED);
        if (finalAnswer != null) {
            output.setAnswer(finalAnswer.toString());
        }
        if (ctx.isWaiting()) {
            output.setCompleted(false);
            output.setWaitKey(ctx.getWaitKey());
        }
        return output;
    }

    private AgentOutput buildErrorOutput(WorkflowContext<AgentInput> ctx, Throwable error) {
        AgentOutput output = new AgentOutput();
        output.setSessionId(ctx.getWorkflowInstanceId());
        output.setCompleted(true);
        output.setAnswer("Error: " + error.getMessage());
        return output;
    }
}
