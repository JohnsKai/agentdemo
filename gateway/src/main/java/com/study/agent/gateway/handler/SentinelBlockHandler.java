package com.study.agent.gateway.handler;


import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.BlockRequestHandler;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义的sentinel处理器
 *
 * @date 2026/06/06
 **/

@Component
// 声明优先级是当后续网关变复杂，并且若引入其他三方库中的BlockRequestHandler依赖，也能让sentinel能始终正确被优先处理
@Order(0)
public class SentinelBlockHandler implements BlockRequestHandler {
    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = (String) exchange.getAttributes().get("traceId");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 429);
        body.put("message", "请求被限流或熔断: " + ex.getClass().getSimpleName());
        body.put("traceId", traceId);
        body.put("timestamp", System.currentTimeMillis());

        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}
