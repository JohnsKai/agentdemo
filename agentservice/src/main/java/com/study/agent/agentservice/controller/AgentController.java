package com.study.agent.agentservice.controller;

import com.study.agent.agentservice.model.AgentInput;
import com.study.agent.agentservice.model.AgentOutput;
import com.study.agent.agentservice.service.AgentWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @date 2026/06/06
 **/
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);
    private final AgentWorkflowService workflowService;

    public AgentController(AgentWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AgentOutput> chat(@RequestBody AgentInput input,
                                  @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        // 使用 final 变量存储最终 traceId
        final String finalTraceId = (traceId == null) ? UUID.randomUUID().toString() : traceId;
        log.info("[{}] Received chat request: sessionId={}, message={}",
                traceId, input.getSessionId(), input.getMessage());

        if (input.getSessionId() == null || input.getSessionId().isBlank()) {
            input.setSessionId(UUID.randomUUID().toString());
        }

        return workflowService.start(input)
                .doOnSuccess(output -> log.info("[{}] Workflow completed: {}", finalTraceId, output))
                .doOnError(e -> log.error("[{}] Workflow failed", finalTraceId, e))
                .onErrorResume(e -> {
                    // 出错时返回一个 completed=true 的 AgentOutput，answer 字段包含错误信息
                    AgentOutput errorOutput = new AgentOutput();
                    errorOutput.setSessionId(input.getSessionId());
                    errorOutput.setCompleted(true);
                    errorOutput.setAnswer("Internal server error: " + e.getMessage());
                    return Mono.just(errorOutput);
                });
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}
