package com.study.agent.agentservice.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;

public class MockMcpServer {
    private static MockWebServer server;
    private static ObjectMapper mapper = new ObjectMapper();

    public static void start() throws IOException {
        server = new MockWebServer();
        server.start();
        enqueueToolsList();
        enqueueToolCallResult(2);
    }

    private static void enqueueToolsList() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", "1");
        ObjectNode result = response.putObject("result");
        ArrayNode tools = result.putArray("tools");

        ObjectNode calculator = tools.addObject();
        calculator.put("name", "calculator");
        calculator.put("description", "数学计算");
        ObjectNode inputSchema = calculator.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode properties = inputSchema.putObject("properties");
        ObjectNode exprProp = properties.putObject("expr");
        exprProp.put("type", "string");
        ArrayNode required = inputSchema.putArray("required");
        required.add("expr");

        server.enqueue(new MockResponse()
                .setBody(response.toString())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON));
    }

    private static void enqueueToolCallResult(int resultValue) throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", "2");
        ObjectNode result = response.putObject("result");
        ArrayNode content = result.putArray("content");
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", "计算结果: " + resultValue);

        server.enqueue(new MockResponse()
                .setBody(response.toString())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON));
    }

    public static String getUrl() {
        return server.url("/").toString();
    }

    public static void stop() throws IOException {
        server.shutdown();
    }
}
