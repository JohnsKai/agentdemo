package com.study.agent.agentcore.context;

import com.study.agent.agentcore.model.Message;
import com.study.agent.agentcore.model.WorkflowId;
import com.study.agent.agentcore.model.WorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface WorkflowContext<I> {
    WorkflowId getWorkflowId();

    String getWorkflowInstanceId();

    I getInput();

    Map<String, Object> getVariables();

    void removeVariable(String key);

    void setVariable(String key, Object value);

    int getCurrentStep();

    void incrementStep();

    WorkflowStatus getStatus();

    void setStatus(WorkflowStatus status);

    boolean isWaiting();

    void setWaiting(boolean waiting);

    String getWaitKey();

    void setWaitKey(String waitKey);

    Throwable getError();

    void setError(Throwable error);

    Instant getCreatedAt();

    Instant getLastUpdatedAt();

    void touch();

    List<Message> getConversationHistory();

    void addMessage(Message msg);
}
