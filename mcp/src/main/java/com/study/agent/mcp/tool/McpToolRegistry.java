package com.study.agent.mcp.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.mcp.client.McpClient;
import com.study.agent.mcp.client.McpClientConfig;
import com.study.agent.mcp.model.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public class McpToolRegistry implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);
    private final McpClient mcpClient;

    private final ToolRegistry toolRegistry;
    private final McpClientConfig config;

    private final ObjectMapper objectMapper;

    public McpToolRegistry(McpClient mcpClient,
                           ToolRegistry toolRegistry,
                           McpClientConfig config,
                           ObjectMapper objectMapper) {
        this.mcpClient = mcpClient;
        this.toolRegistry = toolRegistry;
        this.config = config;
        this.objectMapper = objectMapper;
    }


    @Override
    public void run(String... args) {
        if (!config.isEnabled()) return;
        log.info("Fetching MCP tools from {}", config.getServerUrl());
        fetchAndRegisterTools().block(); // 阻塞直到注册完成，避免并发问题
    }

    private Mono<Void> fetchAndRegisterTools() {
        return mcpClient.call("tools/list", Collections.emptyMap())
                .map(response -> response.path("tools"))
                .flatMapMany(toolsNode -> {
                    try {
                        // 使用 ObjectMapper 将 JsonNode 解析为 List<McpTool>
                        List<McpTool> tools = objectMapper.convertValue(toolsNode,
                                new TypeReference<List<McpTool>>() {
                                });
                        return Mono.just(tools);
                    } catch (Exception e) {
                        log.error("Failed to parse MCP tools response", e);
                        return Mono.error(e);
                    }
                })
                .flatMapIterable(list -> list)
                .doOnNext(tool -> {
                    McpToolAdapter adapter = new McpToolAdapter(tool, mcpClient);
                    toolRegistry.register(adapter);
                    log.info("Registered MCP tool: {}", tool.getName());
                })
                .then();
    }
}
