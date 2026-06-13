package com.study.agent.agentservice.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "agent.llm.provider", havingValue = "deepseek")
public class DeepSeekLLMClient implements LLMClient {
    @Override
    public Mono<String> chat(String prompt) {
        // 模拟 LLM 返回简单决策
//        if (StringUtils.isNotBlank(prompt) && prompt.contains("工具")) {
//            return Mono.just("{\"type\":\"tool_call\",\"tool\":\"calculator\",\"args\":{\"expr\":\"1+1\"}}");
//        } else {
//            return Mono.just("{\"type\":\"final_answer\",\"answer\":\"这是 Mock 回复\"}");
//        }

        return Mono.just("{\"type\":\"final_answer\",\"answer\":\"这是 Mock 回复\"}");
    }
}
