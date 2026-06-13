package com.study.agent.agentcore.tool;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface Tool {
    String getName();

    String getDescription();

    Map<String, Object> getParametersSchema(); // JSON Schema 格式

    /**
     * 是否需要用户确认后才能执行
     * 默认返回 false，敏感工具可重写为 true
     */
    default boolean requiresConfirmation() {
        return false;
    }

    Mono<Object> execute(Map<String, Object> args);
}
