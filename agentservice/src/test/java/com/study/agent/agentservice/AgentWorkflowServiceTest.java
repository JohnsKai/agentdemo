package com.study.agent.agentservice;

import com.study.agent.agentcore.store.WorkflowStateStore;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.llm.LLMClient;
import com.study.agent.agentservice.model.AgentInput;
import com.study.agent.agentservice.model.AgentOutput;
import com.study.agent.agentservice.service.AgentWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AgentWorkflowServiceTest {

    @MockBean
    private LLMClient llmClient;

    @MockBean
    private WorkflowStateStore stateStore;

    @Autowired
    private ToolRegistry toolRegistry; // 真实实现（例如 DefaultToolRegistry）

    @Autowired
    private AgentWorkflowService service;

    @Test
    void testStartWithFinalAnswer() {
        when(llmClient.chat(anyString()))
                .thenReturn(Mono.just("{\"action\":{\"type\":\"final_answer\",\"answer\":\"hello from mock\"}}"));

        AgentInput input = new AgentInput();
        input.setMessage("hi");
        StepVerifier.create(service.start(input))
                .assertNext(new Consumer<AgentOutput>() {
                    @Override
                    public void accept(AgentOutput output) {
                        assertThat(output.isCompleted()).isTrue();
                        assertThat(output.getAnswer()).isEqualTo("hello from mock");
                    }
                })
                .verifyComplete();
    }
}
