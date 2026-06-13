package com.study.agent.agentcore.model;

import java.time.Instant;
import java.util.Map;

/**
 * 单条消息
 */
public class Message {
    private String role;    // "user" 或 "assistant" 或 "tool"
    private String content;
    private Instant timestamp;
    private String toolCallId; // 如果 role 为 tool，关联的工具调用ID

    private Map<String, Object> metadata;

    // 构造器、getter、setter 省略...


    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public Message(String role, String content, Instant timestamp, String toolCallId, Map<String, Object> metadata) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.toolCallId = toolCallId;
        this.metadata = metadata;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
