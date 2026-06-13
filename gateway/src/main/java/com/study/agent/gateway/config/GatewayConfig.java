package com.study.agent.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.study.agent.gateway.handler.SentinelBlockHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @date 2026/06/06
 **/
@Configuration
public class GatewayConfig {

    @Autowired
    private SentinelBlockHandler sentinelBlockHandler;

    /**
     * 显示注入，让sentinel生效，为每个gateway路由自动创建sentinel资源
     *
     * @return
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }
}
