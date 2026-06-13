package com.study.agent.agentservice.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonParser {
    private static final Logger log = LoggerFactory.getLogger(JsonParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 匹配 JSON 对象或数组的正则（简单版，适用于大多数情况）
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\\s*(\\{.*\\})\\s*", Pattern.DOTALL
    );

    /**
     * 将 LLM 响应字符串解析为 Map<String, Object>
     * 支持：
     * - 纯 JSON 字符串
     * - 被 ```json ... ``` 包裹的 JSON
     * - 前后有额外文本但包含 JSON 对象
     *
     * @param response LLM 原始输出
     * @return 解析后的 Map，若失败则抛出 RuntimeException
     */
    public static Map<String, Object> parseJson(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Response cannot be null or blank");
        }

        String jsonText = extractJson(response);
        try {
            return objectMapper.readValue(jsonText, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse JSON from response: {}", response, e);
            throw new RuntimeException("Invalid JSON response from LLM", e);
        }
    }

    /**
     * 从 LLM 响应中提取 JSON 内容
     */
    private static String extractJson(String response) {
        // 1. 尝试提取 Markdown 代码块中的 JSON
        Pattern codeBlockPattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher codeMatcher = codeBlockPattern.matcher(response);
        if (codeMatcher.find()) {
            return codeMatcher.group(1);
        }

        // 2. 尝试直接匹配整个字符串是否为 JSON 对象
        Matcher jsonMatcher = JSON_PATTERN.matcher(response);
        if (jsonMatcher.matches()) {
            return jsonMatcher.group(1);
        }

        // 3. 尝试找到第一个 '{' 和最后一个 '}' 之间的内容（适用于前后有文字的情况）
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }

        throw new IllegalArgumentException("No JSON object found in response: " + response);
    }
}
