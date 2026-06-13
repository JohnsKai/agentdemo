# Agent

## 项目简介

本项目是一个**可扩展的智能体（Agent）服务平台**，基于 Java 17 + Spring Boot 2.7.x + Project Reactor 构建。它实现了 ReAct（推理-行动）模式的工作流引擎，支持工具调用、MCP（Model Context Protocol）协议集成，并提供了 API 网关、限流熔断、全链路追踪等微服务治理能力。

**核心价值**：让开发者能够快速构建自主决策、可观测的AI 智能体应用。

---

## 架构概览

项目采用多模块 Maven 结构，各模块职责：
agentdemo/ 

├── agent-core/ # 工作流核心抽象（无业务，无Spring依赖）

├── agent-service/ # 业务实现（决策、工具、REST API）

├── agent-gateway/ # API网关（限流、熔断、路由、TraceId）

└── mcp/ # MCP客户端集成（动态工具发现与调用）

**依赖关系**：
- `agent-service` → `agent-core`
- `mcp` → `agent-core`
- `agent-gateway` 独立，通过 HTTP 转发请求到 `agent-service`

**数据流简图**：

用户 → Gateway（限流/熔断）→ Agent Service（工作流引擎）→ LLM（决策）

↓

Tool / MCP（执行动作）

↓

返回最终答案 → 用户

---

## 核心特性

### ✅ 工作流引擎（agent-core）

- **ReAct 模式**：支持 `思考 → 行动 → 观察` 循环，由 LLM 自主决策下一步。
- **状态管理**：`WorkflowContext` 存储变量、对话历史、步骤计数、等待状态。
- **异步持久化**：通过 `WorkflowStateStore` 接口支持等待时的快照存储（内存/Redis）。
- **事件驱动**：工作流执行过程中产生 `WorkflowEvent`，便于日志、监控、审计。
- **泛型设计**：输入类型 `I` 可自定义，输出通过 `StepResult.finalOutput` 传递。

### ✅ 业务实现（agent-service）

- **LLM 决策**：`LLMDecisionAction` 调用 LLM（支持 Mock / DeepSeek / OpenAI），解析 `thought` 和 `action`。
- **工具调用**：`ToolCallAction` 通过 `ToolRegistry` 执行本地或远程工具，支持用户确认。
- **人类交互**：`ask_user` 动作使工作流挂起，等待外部输入后恢复。(待验证)
- **REST API**：`/chat`（开始会话）、`/resume/{waitKey}`（恢复等待）。
- **全链路追踪**：通过 `X-Trace-Id` 头透传，日志自动关联。

### ✅ API 网关（agent-gateway）

- **限流熔断**：集成 Sentinel，支持全局限流、用户级限流、慢调用熔断。
- **自定义降级**：统一 JSON 响应格式，包含 `traceId` 和错误信息。
- **TraceId 生成与透传**：自动生成或继承上游 ID，写入 MDC 并向下游传递。
- **Prometheus 指标**：预留 `/actuator/prometheus` 端点 (暂未显示集成，仅加入了依赖)

### ✅ MCP 集成（mcp）

- **MCP 客户端**：基于 WebClient + JSON-RPC 2.0，支持 `tools/list` 和 `tools/call`。
- **动态工具注册**：启动时从 MCP Server 拉取工具列表，自动创建 `McpToolAdapter` 并注册到 `ToolRegistry`。
- **统一工具接口**：MCP 工具与本地 Skill 完全一致，对工作流透明。

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- (可选) Redis（用于分布式状态存储）
- (可选) MCP Server（如 `npx -y @modelcontextprotocol/server-everything`）

### 构建项目

```bash
git clone <your-repo>
cd agentdemo
mvn clean install
```

### 运行 Agent Service
```bash
java -jar agent-service/target/agent-service-1.0.0.jar

默认8081端口
curl -X POST http://localhost:8081/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"1+1等于多少？"}'
```

### 运行网关（可选）
```bash
java -jar agent-gateway/target/agent-gateway-1.0.0.jar

默认端口9000
网关会将 /api/agent/** 路由到 http://localhost:8081
```

### 配置文件示例（agent-service）
```yaml
server:
  port: 8081
agent:
  max-steps: 10                # 工作流最大步数
  llm:
    provider: mock             # mock / deepseek
    deepseek:
      api-key: your-key
      endpoint: https://api.deepseek.com/v1/chat/completions
  mcp:
    enabled: false             # 是否启用 MCP 工具发现
    server-url: http://localhost:3000   # MCP Server 地址

spring:
  redis:
    host: localhost            # 用于状态存储（可选）
```

## 扩展开发

### 添加自定义工具（Skill）

#### 实现 Tool 接口：

```java
@Component
public class MyTool implements Tool {
    @Override public String getName() { return "my_tool"; }
    @Override public String getDescription() { return "我的自定义工具"; }
    @Override public Map<String, Object> getParametersSchema() { return Map.of(...) };
    @Override public boolean requiresConfirmation() { return false; }
    @Override public Mono<Object> execute(Map<String, Object> args) {
        return Mono.just("执行结果");
    }
}
```
确保该类被 Spring 扫描，或手动注册到 ToolRegistry。

### 接入新的 LLM
实现 LLMClient 接口。

根据配置条件注册 Bean（使用 @ConditionalOnProperty）。

### 自定义条件（Condition）
实现 Condition<I> 接口，并在创建 WorkflowExecutor 时传入。

### 自定义动作（Action）
实现 Action<I> 接口，用于决策或工具调用。

## 模块详解
agent-core
engine：WorkflowExecutor（一步执行）、WorkflowScheduler（驱动循环）

context：WorkflowContext 接口及 InMemoryWorkflowContext (暂时用本地map来实现)

model：WorkflowId、WorkflowStatus、ActionResult、WorkflowEvent、Message、StepResult等

store：WorkflowStateStore 接口（用于持久化等待状态）

tool：Tool 接口、ToolRegistry 接口

agent-service
llm：LLMClient 接口、MockLLMClient、DeepSeekLLMClient

action：LLMDecisionAction、ToolCallAction

condition：MaxStepsCondition

service：AgentWorkflowService（对外接口）等

controller：AgentController

store：RedisWorkflowStateStore（可选）

mcp
client：McpClient（HTTP JSON-RPC）、McpClientConfig

tool：McpToolAdapter、McpToolRegistry（CommandLineRunner）

config：McpAutoConfiguration

agent-gateway
filter：TraceIdFilter、UserRateLimitFilter

handler：SentinelBlockHandler

config：SentinelRuleConfig、GatewayConfig
## 后续规划
提供 Web UI 可视化工作流

集成更多 MCP Server（文件系统、数据库等）

流式输出 LLM 响应

支持更多 LLM 提供商（Azure OpenAI、Anthropic）
