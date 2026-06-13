package com.study.agent.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * MCP 协议客户单(暂定JSON-RPC方式)
 */
public class McpClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpClient(String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public McpClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 调用 MCP 方法（JSON-RPC）
     *
     * @param method 方法名，如 "tools/list"
     * @param params 参数
     * @return 响应 result 字段
     */
    public Mono<JsonNode> call(String method, Map<String, Object> params) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", method);
        request.set("params", objectMapper.valueToTree(params));

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    if (response.has("error")) {
                        throw new RuntimeException("MCP error: " + response.get("error"));
                    }
                    return response.get("result");
                });
    }
}
