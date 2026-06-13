package com.study.agent.agentcore.model;

/**
 * 当前workflow 运行状态：运行中，等待，完成，失败
 */
public enum WorkflowStatus {
    RUNNING, WAITING, COMPLETED, FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}