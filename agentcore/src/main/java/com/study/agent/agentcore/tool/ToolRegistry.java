package com.study.agent.agentcore.tool;

import java.util.Collection;

public interface ToolRegistry {
    void register(Tool tool);

    Tool getTool(String name);

    Collection<Tool> getAllTools();

    String getToolsJsonSchema();

    String getHumanReadableTools();
}
