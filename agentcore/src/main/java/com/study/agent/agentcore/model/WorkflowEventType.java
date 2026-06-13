package com.study.agent.agentcore.model;

public enum WorkflowEventType {
    // 开始执行一个新步骤
    STEP_START,
    // 步骤执行结束
    STEP_END,
    // 调用某个动作（Action）
    ACTION_CALL,
    // 评估条件
    CONDITION_EVAL,
    // 进入等待状态（异步等待外部事件）
    WAITING,
    // 从等待中恢复
    RESUMED,
    //  工作流正常完成
    COMPLETED,
    // 工作流失败（抛异常）
    FAILED
}
