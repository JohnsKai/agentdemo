package com.study.agent.agentservice.persistence;

import com.study.agent.agentservice.domain.AgentSession;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 这里模拟持久化+快照，暂时用map来存储，仅仅为了演示和学习
 *
 * @date 2026/06/06
 **/
@Component
public class InMemorySessionStore {

    private final ConcurrentHashMap<String, AgentSession> sessions = new ConcurrentHashMap<>();

    public Mono<AgentSession> load(String sessionId) {
        return Mono.justOrEmpty(sessions.get(sessionId));
    }

    public Mono<Void> save(AgentSession session) {
        sessions.put(session.getSessionId(), session);
        return Mono.empty();
    }

    public Mono<Boolean> exists(String sessionId) {
        return Mono.just(sessions.containsKey(sessionId));
    }
}
