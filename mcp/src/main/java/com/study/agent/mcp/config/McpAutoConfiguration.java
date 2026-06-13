package com.study.agent.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.mcp.client.McpClient;
import com.study.agent.mcp.client.McpClientConfig;
import com.study.agent.mcp.tool.McpToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(McpClientConfig.class)
@ConditionalOnClass(McpClient.class)                   // 确保 MCP 相关类存在
@ConditionalOnBean(ToolRegistry.class)                 // 确保容器中有 ToolRegistry 实例
@ConditionalOnProperty(name = "agent.mcp.enabled", havingValue = "true")
public class McpAutoConfiguration {
    @Bean
    public McpClient mcpClient(McpClientConfig config, WebClient.Builder webclientBuilder) {
        WebClient webClient = webclientBuilder.baseUrl(config.getServerUrl()).build();
        return new McpClient(webClient);
    }

    @Bean
    public McpToolRegistry mcpToolRegistry(McpClient mcpClient,
                                           @Autowired(required = false) ToolRegistry toolRegistry,
                                           McpClientConfig config,
                                           ObjectMapper objectMapper) {
        return new McpToolRegistry(mcpClient, toolRegistry, config, objectMapper);
    }
}
