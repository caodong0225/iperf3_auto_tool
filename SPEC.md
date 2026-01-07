# 软件需求规格说明书 (Software Requirements Specification)

**版本: 1.1**

**日期: 2026-01-07**

---

## 1. 项目概述

本项目旨在开发一款基于 Java Swing 的图形化桌面应用，用于自动化 `iperf3` 网络性能测试。软件通过 SSH 连接并控制远程 Linux 服务器，允许用户在 GUI 界面上方便地配置测试参数、执行测试、并收集结果。

**核心目标:**
- **易用性:** 提供简洁直观的图形化操作界面，降低 `iperf3` 使用门槛。
- **自动化:** 自动完成 SSH 连接、环境检查、命令执行、进程清理等繁琐步骤。
- **健壮性:** 具备完善的错误处理和进程管理机制，确保软件运行稳定。
- **可扩展性:** 采用模块化和接口驱动的设计，为未来功能扩展（如报表分析）奠定基础。

---

## 2. 技术栈与架构

### 2.1. 技术选型

| 类别 | 技术 | 版本/说明 |
| :--- | :--- | :--- |
| **项目管理** | Maven | 标准 Java 项目管理工具 |
| **语言/环境** | JDK | 17 |
| **GUI 框架** | Swing | Java 原生 GUI 框架 |
| **GUI 外观** | FlatLaf | 3.4.1+ (提供现代化、跨平台的界面外观) |
| **SSH 库**| JSch | 0.1.55 (用于 SSH 连接与远程命令执行) |
| **辅助工具** | Lombok | 1.18.30+ (简化 JavaBean 代码) |
| **日志框架** | SLF4J + Logback | 标准日志门面与实现，用于记录日志 |

### 2.2. 架构设计原则

软件开发将遵循以下设计模式，以确保代码的规范性、可读性和可扩展性。

- **单例模式 (Singleton):** 用于需要全局唯一实例的服务，如 `ServerProfileService` (管理历史服务器配置) 和 `AppConfig` (管理全局应用配置)，确保数据一致性。
- **工厂模式 (Factory):** 用于创建 `iperf3` 命令对象 (`IperfCommandFactory`)。根据用户在 UI 上的配置，动态构建出对应的命令行字符串，实现命令构建逻辑与业务逻辑的分离。
- **责任链模式 (Chain of Responsibility):** 用于构建模块化、可扩展的测试执行流程。请求将通过一系列处理器（如输入校验、SSH连接、服务启动、客户端执行、结果解析、资源清理），每个处理器各司其职，高度解耦。

---

## 3. 核心数据模型 (Data Models)

为确保数据结构的清晰与一致，定义以下核心模型（使用 Lombok 注解简化）。

### 3.1. `ServerProfile`
代表一个已保存的服务器连接配置。

```java
@Data // Lombok: auto-generates getters, setters, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
public class ServerProfile {
    private String profileName; // e.g., "主数据中心服务器"
    private String host;
    private int port;
    private String username;
    private String password;
}
```

### 3.2. `TestConfig`
代表一次测试的所有配置参数。

```java
@Data
@Builder // Lombok: provides builder pattern
public class TestConfig {
    private ServerProfile serverProfile;
    private String clientBindAddress;
    private String serverBindAddress;
    private int testPort;
    private String protocol; // "TCP" or "UDP"
    private int duration; // seconds
    // ... 其他 iperf3 参数
}
```

### 3.3. `TestResult`
代表一次测试的最终结果，将序列化为 JSON 文件。

```java
@Data
@Builder
public class TestResult {
    private String testId; // UUID
    private String testTimestamp; // ISO 8601 format
    private TestConfig configuration; // 本次测试的完整配置
    private IperfOutput summary; // iperf3 的摘要输出
    private boolean success;
    private String errorMessage; // 如果测试失败，记录错误信息

    // iperf3 摘要数据模型
    @Data
    public static class IperfOutput {
        private double bandwidthMbps;
        private double transferGigabytes;
        private double jitterMs;
        private int lostPackets;
        private int totalPackets;
        // ... 其他 iperf3 结果字段
    }
}
```

---

## 4. 功能需求 (Functional Requirements)

### 4.1. 主界面 UI 布局
- **主窗口 (`JFrame`):** 包含菜单栏、主体内容区和状态栏。
- **菜单栏 (`JMenuBar`):** 包含 "文件" -> "打开结果目录", "帮助" -> "关于"。
- **主体内容区 (`JSplitPane`):**
    - **左侧配置面板:** 采用上下布局。
        - **上半区 (客户端配置):** 自动检测并以下拉列表形式展示本地所有网卡 IP，供用户选择。
        - **下半区 (服务端配置):** 包含 SSH 服务器 IP、用户名、密码输入框。
    - **右侧结果面板:** 使用 `JTextPane` 显示格式化的测试摘要报告。
- **状态栏 (`JPanel`):** 位于窗口底部，用于显示程序当前状态和简短信息。

### 4.2. UI 交互状态与校验

| 操作/状态 | 关联组件 | 行为描述 |
| :--- | :--- | :--- |
| **程序启动时** | “开始测试”按钮 | 默认为禁用，直到 SSH 连接成功。 |
| **连接/测试中** | 所有配置输入框、连接按钮、“开始测试”按钮 | 全部禁用，防止用户修改参数。 |
| **测试完成/失败** | 所有配置输入框、连接按钮、“开始测试”按钮 | 全部恢复为可用状态。 |
| **输入校验** | IP, 端口等输入框 | 失去焦点时触发校验。若格式错误，边框变红并显示`ToolTip`提示错误信息。“开始测试”按钮保持禁用。 |
| **状态栏提示** | 状态栏文本 | 实时更新，显示如: "准备就绪", "正在连接 SSH...", "SSH 连接成功", "正在执行测试...", "测试完成", "错误: 连接超时"。|

### 4.3. 服务器配置记忆功能
- 在服务端配置区，提供一个下拉列表以选择“已保存的服务器配置”。
- 提供“保存”按钮，将当前输入的服务器信息持久化存储到本地 `profiles.json` 文件。若配置名称已存在，则提示用户是否覆盖。
- 提供“删除”按钮，可删除已保存的服务器配置。

### 4.4. 测试执行与进程管理
- **执行流程:** 严格遵循责任链模式中定义的流程。
- **进程清理:** 强制实现“PID记录与清理”策略。通过注册 JVM Shutdown Hook，确保在程序任何形式的退出（正常或异常）时，都最大限度地尝试清理残留在服务端的 `iperf3` 孤儿进程。

### 4.5. 结果处理与持久化
- 测试结束后，在右侧结果面板显示格式化的摘要报告。
- 每次测试完成后，将 `TestResult` 对象序列化为 **JSON 格式**，保存到项目根目录下的 `test_results/` 文件夹中，文件名格式为 `[时间戳ISO格式]_[服务端IP].json`。

---

## 5. 非功能性需求 (Non-Functional Requirements)

### 5.1. 健壮性与错误处理详细策略

| 场景 | 策略 |
| :--- | :--- |
| SSH 连接失败 (网络/认证) | 在右侧结果面板和状态栏显示 JSch 抛出的**原始技术性错误信息**。 |
| `iperf3` 命令不存在 | 同上，视为远程命令执行失败，显示原始错误。 |
| 远程命令需要 `sudo` | 尝试使用用户提供的当前 SSH 密码作为 `sudo` 密码。如果失败，显示原始错误。 |
| 远程命令执行超时 | 设置10秒超时。如果超时，报告“命令执行超时”错误。 |
| `profiles.json` 读写失败 | 在状态栏提示错误，并可选择忽略或重试。 |

### 5.2. 日志策略 (Logging)

- 引入 **SLF4J + Logback** 日志框架。
- **日志级别:**
    - `INFO`: 记录关键生命周期事件（程序启动/关闭，测试开始/结束）。
    - `DEBUG`: 记录详细的 SSH 命令、参数、`iperf3` 原始输出（用于调试）。
    - `ERROR`: 记录所有捕获到的异常和错误信息。
- **日志输出:**
    - **控制台:** 在开发时，输出 `DEBUG`及以上级别日志。
    - **文件:** 在程序运行目录下创建 `logs/app.log` 文件，记录 `INFO` 及以上级别日志，并按天滚动。

### 5.3. 交付物
- 最终交付物为一个可直接运行的 `.jar` 包，所有依赖（JSch, FlatLaf 等）均被打包在内。

---

## 6. 未来扩展 (当前版本不实现)

- SSH 公钥认证。
- `iperf3` CPU 占用率统计 (待用户提供实现方法)。
- 历史测试报告的统计与可视化分析功能。
- 支持并发测试多台服务器。
- 国际化支持（中/英文切换）。

---
