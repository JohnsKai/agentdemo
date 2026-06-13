package com.study.agent.agentservice.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 调用llm大模型
 *
 * @date 2026/06/06
 **/
@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);


    // 正常业务方法
    @SentinelResource(value = "callLLM", fallback = "callLLMFallback")
    public Mono<String> callLLM(String userMessage) {
        // 模拟调用大模型 API，可能超时或抛出异常
        if (userMessage.contains("error")) {
            throw new RuntimeException("大模型服务异常");
        }
        // 正常调用逻辑
        return Mono.just("大模型回复: " + userMessage);
    }

    // 降级方法（必须与原始方法返回类型相同，参数一致，可多一个 Throwable 参数）
    public Mono<String> callLLMFallback(String userMessage, Throwable ex) {
        // 记录日志
        log.error("降级触发，原因: " + ex.getMessage());
        // 返回默认回复
        return Mono.just("抱歉，服务繁忙，请稍后再试。");
    }
}
