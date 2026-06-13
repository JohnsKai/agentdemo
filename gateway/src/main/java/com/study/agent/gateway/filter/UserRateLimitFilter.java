package com.study.agent.gateway.filter;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * @date 2026/06/06
 **/
@Component
public class UserRateLimitFilter implements GlobalFilter, Ordered {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (StringUtils.isBlank(userId)) {
            userId = "anonymous";
        }
        String resourceName = "user-limit";

        Entry entry = null;
        try {
            // 指定传入的userId作为热点参数(索引0)
            entry = SphU.entry(resourceName, EntryType.IN, 1, userId);
            return chain.filter(exchange);
        } catch (BlockException e) {
            return handleBlock(exchange, e);
        } finally {
            if (entry != null) {
                entry.exit(1, userId);
            }
        }
    }

    private Mono<Void> handleBlock(ServerWebExchange exchange, BlockException e) {
        String traceId = (String) exchange.getAttributes().get("traceId");
        Map<String, Object> body = new HashMap<>();
        body.put("code", 429);
        body.put("message", "尊敬的顾客，当前您请求过于频繁，请稍后再试");
        body.put("traceId", traceId);
        body.put("timestamp", System.currentTimeMillis());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }


}
