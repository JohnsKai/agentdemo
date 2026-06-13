package com.study.agent.agentcore.model;

import java.util.List;
import java.util.Optional;

public class StepResult {
    private final List<WorkflowEvent> events;
    private final WorkflowStatus newStatus;
    private final Object finalOutput;  // 新增：工作流最终输出（仅在 COMPLETED 时有值）

    private final String waitKey;   // 新增

    public StepResult(List<WorkflowEvent> events, WorkflowStatus newStatus) {
        this(events, newStatus, null, null);
    }

    public StepResult(List<WorkflowEvent> events, WorkflowStatus newStatus, Object finalOutput) {
        this(events, newStatus, finalOutput, null);
    }

    public StepResult(List<WorkflowEvent> events, WorkflowStatus newStatus, Object finalOutput, String waitKey) {
        this.events = events;
        this.newStatus = newStatus;
        this.finalOutput = finalOutput;
        this.waitKey = waitKey;
    }

    public List<WorkflowEvent> getEvents() {
        return events;
    }

    public WorkflowStatus getNewStatus() {
        return newStatus;
    }

    public Optional<Object> getFinalOutput() {
        return Optional.ofNullable(finalOutput);
    }

    public Optional<String> getWaitKey() {
        return Optional.ofNullable(waitKey);
    }
}
