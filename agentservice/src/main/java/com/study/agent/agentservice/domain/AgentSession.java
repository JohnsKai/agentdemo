package com.study.agent.agentservice.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话状态(快照)
 *
 * @date 2026/06/06
 **/
public class AgentSession {
    private String sessionId;
    private String traceId;
    // 简化历史
    private List<String> history = new ArrayList<>();
    private int stepCount;
    private String lastObservation;
    private Instant lastActiveTime;
    private boolean finished;
    private String finalAnswer;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public String getLastObservation() {
        return lastObservation;
    }

    public void setLastObservation(String lastObservation) {
        this.lastObservation = lastObservation;
    }

    public Instant getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Instant lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }
}
