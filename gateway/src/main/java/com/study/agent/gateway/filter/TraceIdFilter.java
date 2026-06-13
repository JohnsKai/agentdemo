package com.study.agent.gateway.filter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * GlobalFilter 全局过滤器
 *
 * @date 2026/06/06
 **/
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID_MDC, traceId);
        exchange.getAttributes().put(TRACE_ID_MDC, traceId);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(TRACE_ID_HEADER, traceId)
                        .build())
                .build();

        return chain.filter(mutatedExchange)
                // 及时清理，避免线程切换导致traceId污染，保证每个线程能正确传递
                .doFinally(signal -> MDC.remove(TRACE_ID_MDC));
    }

    /**
     * 确保filter在整个请求处理链的最早阶段就生成traceId,并放入MDC
     *
     * @return
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
