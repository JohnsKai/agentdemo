package com.study.agent.agentservice.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.agent.agentcore.tool.Tool;
import com.study.agent.agentcore.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class DefaultToolRegistry implements ToolRegistry {
    private static final Logger log = LoggerFactory.getLogger(DefaultToolRegistry.class);
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    @Override
    public void register(Tool tool) {
        String name = tool.getName();
        if (tools.containsKey(name)) {
            log.warn("Tool with name '{}' already exists, will be overwritten", name);
        }
        tools.put(name, tool);
        log.info("Registered tool: {}", name);
    }

    @Override
    public Tool getTool(String name) {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }
        return tool;
    }

    @Override
    public Collection<Tool> getAllTools() {
        return tools.values();
    }

    @Override
    public String getToolsJsonSchema() {
        List<Map<String, Object>> toolsList = new ArrayList<>();
        for (Tool tool : tools.values()) {
            Map<String, Object> toolDef = new LinkedHashMap<>();
            toolDef.put("name", tool.getName());
            toolDef.put("description", tool.getDescription());
            toolDef.put("parameters", tool.getParametersSchema());
            toolsList.add(toolDef);
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(toolsList);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tools schema", e);
            return "[]";
        }
    }

    /**
     * 生成人类可读的工具列表描述（用于文本 prompt）
     */
    @Override
    public String getHumanReadableTools() {
        StringBuilder sb = new StringBuilder();
        for (Tool tool : tools.values()) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
            sb.append("  参数: ").append(tool.getParametersSchema()).append("\n");
        }
        return sb.toString();
    }
}