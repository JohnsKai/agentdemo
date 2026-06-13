package com.study.agent.agentservice.domain;

import java.time.Instant;

/**
 * 事件模型(需要持久化)
 *
 * @date 2026/06/06
 **/
public class AgentEvent {
    private String sessionId;
    private long sequence;
    private EventType type;
    private String content;
    private Instant timestamp;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public enum EventType {
        USER_INPUT,
        AGENT_THOUGHT,
        TOOL_CALL,
        TOOL_OBSERVATION,
        FINAL_ANSWER
    }
}
