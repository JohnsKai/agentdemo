package com.study.agent.agentservice.llm;

import reactor.core.publisher.Mono;

public interface LLMClient {
    Mono<String> chat(String prompt);
}
