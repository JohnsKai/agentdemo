package com.study.agent.mcp.model;

import java.util.Map;

/**
 * 工具定义(JSON-RPC返回格式)
 */
public class McpTool {
    private String name;
    private String description;
    private Map<String, Object> inputSchema; // JSON Schema

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema;
    }
}
