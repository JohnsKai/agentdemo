package com.study.agent.agentcore.model;

import java.time.Instant;
import java.util.Map;

public record WorkflowEvent(
        String workflowInstanceId,
        WorkflowEventType type,
        Instant timestamp,
        Map<String, Object> details) {

    public static WorkflowEvent stepStart(String instanceId, int step) {
        return new WorkflowEvent(instanceId, WorkflowEventType.STEP_START, Instant.now(),
                Map.of("step", step));
    }

    public static WorkflowEvent stepEnd(String instanceId, int step) {
        return new WorkflowEvent(instanceId, WorkflowEventType.STEP_END, Instant.now(),
                Map.of("step", step));
    }

    public static WorkflowEvent actionCall(String instanceId, String actionName, Object args) {
        return new WorkflowEvent(instanceId, WorkflowEventType.ACTION_CALL, Instant.now(),
                Map.of("action", actionName, "args", args));
    }

    public static WorkflowEvent waiting(String instanceId, String waitKey) {
        return new WorkflowEvent(instanceId, WorkflowEventType.WAITING, Instant.now(),
                Map.of("waitKey", waitKey));
    }

    public static WorkflowEvent resumed(String instanceId, String waitKey) {
        return new WorkflowEvent(instanceId, WorkflowEventType.RESUMED, Instant.now(),
                Map.of("waitKey", waitKey));
    }

    public static WorkflowEvent completed(String instanceId, Object output) {
        return new WorkflowEvent(instanceId, WorkflowEventType.COMPLETED, Instant.now(),
                Map.of("output", output));
    }

    public static WorkflowEvent failed(String instanceId, Throwable error) {
        return new WorkflowEvent(instanceId, WorkflowEventType.FAILED, Instant.now(),
                Map.of("error", error.getMessage()));
    }

}