package com.study.agent.agentservice;

import com.study.agent.mcp.config.McpAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(McpAutoConfiguration.class)
public class AgentserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentserviceApplication.class, args);
    }

}
