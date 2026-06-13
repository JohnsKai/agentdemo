package com.study.agent.agentcore.context;

import com.study.agent.agentcore.model.Message;
import com.study.agent.agentcore.model.WorkflowId;
import com.study.agent.agentcore.model.WorkflowStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWorkflowContext<I> implements WorkflowContext<I> {
    private final WorkflowId workflowId;
    private final String instanceId;
    private final I input;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final List<Message> conversationHistory;
    private final Instant createdAt;
    private int currentStep = 0;
    private WorkflowStatus status = WorkflowStatus.RUNNING;
    private boolean waiting = false;
    private String waitKey;
    private Throwable error;
    private Instant lastUpdatedAt;

    public InMemoryWorkflowContext(WorkflowId workflowId, I input) {
        this.workflowId = workflowId;
        this.instanceId = UUID.randomUUID().toString();
        this.input = input;
        this.conversationHistory = new ArrayList<>();   // 关键：初始化为空列表
        this.createdAt = Instant.now();
        this.lastUpdatedAt = createdAt;
    }

    @Override
    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    @Override
    public String getWorkflowInstanceId() {
        return instanceId;
    }

    @Override
    public I getInput() {
        return input;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public void removeVariable(String key) {
        variables.remove(key);
        touch();
    }

    @Override
    public void setVariable(String key, Object value) {
        variables.put(key, value);
        touch();
    }

    @Override
    public int getCurrentStep() {
        return currentStep;
    }

    @Override
    public void incrementStep() {
        currentStep++;
        touch();
    }

    @Override
    public WorkflowStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(WorkflowStatus status) {
        this.status = status;
        touch();
    }

    @Override
    public boolean isWaiting() {
        return waiting;
    }

    @Override
    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
        touch();
    }

    @Override
    public String getWaitKey() {
        return waitKey;
    }

    @Override
    public void setWaitKey(String waitKey) {
        this.waitKey = waitKey;
        touch();
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    public void setError(Throwable error) {
        this.error = error;
        touch();
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    @Override
    public void touch() {
        this.lastUpdatedAt = Instant.now();
    }

    @Override
    public List<Message> getConversationHistory() {
        return conversationHistory;
    }

    @Override
    public void addMessage(Message msg) {
        conversationHistory.add(msg);
        touch();
    }
}
