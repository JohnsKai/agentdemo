package com.study.agent.agentservice.llm;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockLLMClient implements LLMClient {
    private final List<String> responses = new ArrayList<>();
    private int callCount = 0;

    public MockLLMClient thenReturn(String response) {
        responses.add(response);
        return this;
    }

    @Override
    public Mono<String> chat(String prompt) {
//        if (callCount >= responses.size()) {
//            return Mono.error(new RuntimeException("No more mock responses"));
//        }
        return Mono.just(responses.get(callCount++));
    }
}

