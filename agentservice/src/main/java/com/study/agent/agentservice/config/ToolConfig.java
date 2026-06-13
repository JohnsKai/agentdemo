package com.study.agent.agentservice.config;

import com.study.agent.agentcore.tool.Tool;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.tools.DefaultToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolConfig {

    @Bean
    public ToolRegistry toolRegistry(List<Tool> tools) {
        ToolRegistry registry = new DefaultToolRegistry();
        tools.forEach(registry::register);
        return registry;
    }
}
