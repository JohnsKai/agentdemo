package com.study.agent.agentservice.model;

import com.study.agent.agentcore.model.Message;

import java.util.List;
import java.util.Map;

/**
 * agent 请求输入
 */
public class AgentInput {
    private String sessionId;          // 会话ID（可选，新会话时可自动生成）
    private String message;            // 用户当前消息
    private List<Message> history;     // 对话历史（可选，用于多轮上下文）
    private Map<String, Object> context; // 其他上下文（如用户ID、权限等）

    public AgentInput() {

    }

    public AgentInput(String sessionId, String message, List<Message> history, Map<String, Object> context) {
        this.sessionId = sessionId;
        this.message = message;
        this.history = history;
        this.context = context;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Message> getHistory() {
        return history;
    }

    public void setHistory(List<Message> history) {
        this.history = history;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
