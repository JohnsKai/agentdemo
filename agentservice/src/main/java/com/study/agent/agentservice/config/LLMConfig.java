package com.study.agent.agentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {
    @Value("${agent.llm.provider}")
    private String provider;

}
