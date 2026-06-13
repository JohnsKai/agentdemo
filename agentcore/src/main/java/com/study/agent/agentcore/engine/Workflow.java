package com.study.agent.agentcore.engine;

import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.WorkflowEvent;
import com.study.agent.agentcore.model.WorkflowId;
import org.reactivestreams.Publisher;

public interface Workflow<I, O> {
    WorkflowId id();

    WorkflowContext<I> createContext(I input);

    Publisher<WorkflowEvent> execute(WorkflowContext<I> ctx);
}
