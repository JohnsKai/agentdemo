package com.study.agent.agentservice.model;

import com.study.agent.agentcore.model.Message;

import java.util.List;
import java.util.Map;

/**
 * Agent 响应输出
 */
public class AgentOutput {
    private String sessionId;          // 会话ID
    private String answer;             // 最终答案或回复
    private boolean completed;         // 工作流是否完成（false 表示需要继续等待用户输入）
    private String waitKey;            // 如果需要等待用户输入，返回的等待键
    private List<Message> history;     // 更新后的对话历史（可选）
    private Map<String, Object> metadata; // 其他元数据（如 token 消耗、耗时等）

    public AgentOutput() {
    }

    public AgentOutput(String sessionId, String answer, boolean completed, String waitKey, List<Message> history, Map<String, Object> metadata) {
        this.sessionId = sessionId;
        this.answer = answer;
        this.completed = completed;
        this.waitKey = waitKey;
        this.history = history;
        this.metadata = metadata;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getWaitKey() {
        return waitKey;
    }

    public void setWaitKey(String waitKey) {
        this.waitKey = waitKey;
    }

    public List<Message> getHistory() {
        return history;
    }

    public void setHistory(List<Message> history) {
        this.history = history;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
