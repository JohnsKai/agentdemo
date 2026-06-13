package com.study.agent.agentcore.model;

import java.util.UUID;

/**
 * 不可变类，仅根据workflowid判断是否相等
 *
 * @param value
 */
public record WorkflowId(String value) {

    public static WorkflowId of(String id) {
        return new WorkflowId(id);
    }

    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
