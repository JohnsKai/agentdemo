package com.study.agent.mcp.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置属性(ServerUrl、timeout等)
 */
@ConfigurationProperties(prefix = "agent.mcp")
public class McpClientConfig {
    private String serverUrl = "http://localhost:3000"; // MCP Server 地址
    private int timeoutSeconds = 30;
    private boolean enabled = false;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
