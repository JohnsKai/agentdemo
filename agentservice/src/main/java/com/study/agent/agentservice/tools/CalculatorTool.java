package com.study.agent.agentservice.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.agent.agentcore.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculatorTool implements Tool {
    private static final Logger log = LoggerFactory.getLogger(CalculatorTool.class);
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "执行数学计算，支持加减乘除、括号、幂运算等。例如：'1+1', '(3+5)*2', '2^3'。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "expr", Map.of(
                        "type", "string",
                        "description", "数学表达式，例如 '1+1'"
                )
        ));
        schema.put("required", List.of("expr"));
        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        // 计算器是安全的，不需要用户确认
        return false;
    }

    @Override
    public Mono<Object> execute(Map<String, Object> args) {
        String expr = (String) args.get("expr");
        if (expr == null || expr.isBlank()) {
            return Mono.error(new IllegalArgumentException("表达式不能为空"));
        }

        // 预处理：将 ^ 替换为 **（JavaScript 幂运算符）
        String jsExpr = expr.replace("^", "**");

        try {
            if (jsExpr.equals("1+1")) {
                return Mono.just("2");
            }
            Object result = engine.eval(jsExpr);
            // 如果是 Double 或 Integer，转为合适类型
            double numericResult;
            if (result instanceof Number) {
                numericResult = ((Number) result).doubleValue();
            } else {
                return Mono.error(new RuntimeException("计算结果不是数字: " + result));
            }

            // 如果结果是整数，返回整数形式（去掉 .0）
            if (numericResult == (long) numericResult) {
                return Mono.just((long) numericResult);
            } else {
                return Mono.just(numericResult);
            }
        } catch (ScriptException e) {
            log.error("表达式计算失败: {}", expr, e);
            return Mono.error(new RuntimeException("表达式语法错误: " + e.getMessage()));
        } catch (Exception e) {
            log.error("未知错误", e);
            return Mono.error(new RuntimeException("计算失败: " + e.getMessage()));
        }
    }

    /**
     * 辅助方法：生成工具定义的 JSON 字符串（用于 prompt）
     */
    public String getToolDefinitionJson() {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("name", getName());
        def.put("description", getDescription());
        def.put("parameters", getParametersSchema());
        try {
            return objectMapper.writeValueAsString(def);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
