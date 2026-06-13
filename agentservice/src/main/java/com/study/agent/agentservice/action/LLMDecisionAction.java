package com.study.agent.agentservice.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.agent.agentcore.action.Action;
import com.study.agent.agentcore.context.WorkflowContext;
import com.study.agent.agentcore.model.ActionResult;
import com.study.agent.agentcore.model.Message;
import com.study.agent.agentcore.tool.Tool;
import com.study.agent.agentcore.tool.ToolRegistry;
import com.study.agent.agentservice.llm.LLMClient;
import com.study.agent.agentservice.model.AgentInput;
import com.study.agent.agentservice.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.*;

public class LLMDecisionAction implements Action<AgentInput> {

    private final static Logger logger = LoggerFactory.getLogger(LLMDecisionAction.class);

    private final LLMClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ToolRegistry toolRegistry;

    public LLMDecisionAction(LLMClient llmClient, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
    }

    private String getToolsSchema() {
        Collection<Tool> tools = toolRegistry.getAllTools();
        if (tools.isEmpty()) {
            return "暂无可用工具。";
        }
        List<Map<String, Object>> toolsList = new ArrayList<>();
        for (Tool tool : tools) {
            Map<String, Object> toolDef = new LinkedHashMap<>();
            toolDef.put("name", tool.getName());
            toolDef.put("description", tool.getDescription());
            toolDef.put("parameters", tool.getParametersSchema());
            toolsList.add(toolDef);
        }
        try {
            return objectMapper.writeValueAsString(toolsList);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize tools schema", e);
            return "[]";
        }
    }

    @Override
    public Mono<ActionResult> execute(WorkflowContext<AgentInput> ctx) {
        // 从ctx中获取对话历史
        List<Message> history = ctx.getConversationHistory();
        String prompt = buildPrompt(history, getToolsSchema());
        return llmClient.chat(prompt)
                .map(response -> {
                    Map<String, Object> decision = JsonParser.parseJson(response);
                    String thought = (String) decision.get("thought");
                    Map<String, Object> action = (Map<String, Object>) decision.get("action");
                    String type = (String) action.get("type");

                    // 记录 thought 和 action 到历史
                    history.add(new Message("thought", thought));
                    history.add(new Message("assistant", response)); // 或更细粒度

                    if ("ask_user".equals(type)) {
                        String question = (String) action.get("question");
                        String waitKey = (String) action.get("waitKey");
                        return ActionResult.askUser(question, waitKey);
                    } else if ("tool_call".equals(type)) {
                        return ActionResult.success(action); // 原有逻辑
                    } else if ("final_answer".equals(type)) {
                        return ActionResult.success(action);
                    } else {
                        throw new RuntimeException("Unknown action type");
                    }
                }).onErrorResume(e -> Mono.just(ActionResult.failed(new RuntimeException(e.getMessage()))));
    }

    private String buildPrompt(List<Message> history, String toolsSchema) {
        StringBuilder sb = new StringBuilder();

        // 1. 系统指令
        sb.append("你是一个智能助手，可以调用以下工具来完成用户任务。\n");
        sb.append("请按照以下格式输出你的思考和行动：\n");
        sb.append("Thought: 你对当前情况的思考\n");
        sb.append("Action: 一个JSON对象，包含type字段，可以是 \"tool_call\", \"final_answer\", 或 \"ask_user\"\n");
        sb.append("   - 如果是 tool_call: {\"type\":\"tool_call\", \"tool\":\"工具名\", \"args\":{...}}\n");
        sb.append("   - 如果是 final_answer: {\"type\":\"final_answer\", \"answer\":\"最终答案\"}\n");
        sb.append("   - 如果是 ask_user: {\"type\":\"ask_user\", \"question\":\"提问内容\", \"waitKey\":\"唯一标识\"}\n");
        sb.append("你只需要输出 Thought 和 Action，不要输出多余内容。\n\n");

        // 2. 可用工具列表
        sb.append("## 可用工具\n");
        sb.append(toolsSchema).append("\n\n");

        // 3. 对话历史
        sb.append("## 对话历史\n");
        for (Message msg : history) {
            switch (msg.getRole()) {
                case "user":
                    sb.append("User: ").append(msg.getContent()).append("\n");
                    break;
                case "assistant":
                    sb.append("Assistant: ").append(msg.getContent()).append("\n");
                    break;
                case "tool":
                    sb.append("Tool[").append(msg.getToolCallId()).append("]: ").append(msg.getContent()).append("\n");
                    break;
                case "thought":
                    sb.append("Thought: ").append(msg.getContent()).append("\n");
                    break;
                default:
                    sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
        }

        // 4. 提示 LLM 输出下一步
        sb.append("\n现在请输出你的下一步 Thought 和 Action。\n");
        return sb.toString();
    }
}
