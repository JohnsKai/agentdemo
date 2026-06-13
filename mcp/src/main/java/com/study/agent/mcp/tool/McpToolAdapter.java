package com.study.agent.mcp.tool;

import com.study.agent.agentcore.tool.Tool;
import com.study.agent.mcp.client.McpClient;
import com.study.agent.mcp.model.McpTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class McpToolAdapter implements Tool {
    private final String name;
    private final String description;
    private final Map<String, Object> schema;
    private final McpClient mcpClient;

    public McpToolAdapter(McpTool tool, McpClient mcpClient) {
        this.name = tool.getName();
        this.description = tool.getDescription();
        this.schema = tool.getInputSchema();
        this.mcpClient = mcpClient;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    } // 可根据需要修改

    @Override
    public Mono<Object> execute(Map<String, Object> args) {
        return mcpClient.call("tools/call", Map.of("name", name, "arguments", args))
                .map(result -> result.path("content").toString()); // 提取实际返回内容
    }
}
