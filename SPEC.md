# 软件需求规格说明书 (Software Requirements Specification)

**版本: 3.0**

**日期: 2026-01-08**

---

## 1. 项目概述

### 1.1. 项目目标

本项目旨在开发一款基于 Java Swing 的 **`iperf3` 管理与测试工具**。与传统的成对测试工具不同，本软件将**服务端管理**与**客户端测试**功能完全解耦，为用户提供更大的灵活性。

软件核心功能包括：
1.  **服务端管理器:** 允许用户在任意远程主机上独立的启动、监控和停止 `iperf3` 服务进程。
2.  **客户端测试器:** 允许用户从任意远程主机发起 `iperf3` 客户端测试，可以连接到网络上任何一个 `iperf3` 服务端。
3.  **历史记录浏览器:** 提供一个图形化界面，用于查看和管理所有过往的测试结果。

---

## 2. 技术栈与架构

### 2.1. 技术选型
- **核心语言:** JDK 17
- **图形界面:** Swing, FlatLaf (用于现代化外观)
- **SSH通信:** JSch
- **代码简化:** Lombok
- **日志框架:** SLF4J / Logback
- **JSON处理:** Gson

### 2.2. 架构设计原则
- **解耦设计:** 服务端管理、客户端测试、历史记录查看功能在逻辑和UI上完全分离。
- **面向接口:** 核心服务将通过接口定义，方便未来使用不同实现（如从 JSch 切换到 SSHJ）。
- **设计模式应用:** 在开发过程中，将根据场景合理应用以下设计模式，提升代码质量：
    - **单例 (Singleton):** 用于管理全局唯一服务，如 `ProfileService`。
    - **工厂 (Factory):** 用于创建复杂的对象，如根据不同配置创建 `iperf3` 命令。
    - **策略 (Strategy):** 用于封装不同的测试算法或清理策略。
    - **责任链 (Chain of Responsibility):** 用于处理多步骤的操作流程，如连接、环境检查、执行测试。

---

## 3. 核心数据模型 (Data Models)

### 3.1. `SshProfile`
代表一个远程主机的连接配置。

```java
@Data
@Builder
public class SshProfile {
    private String profileName; // e.g., "北京机房-服务器"
    private String host;
    private int port; // SSH port, default 22
    private String username;
    private String password;
}
```

### 3.2. `IperfServerInstance` (新模型)
代表一个在远程主机上运行的 `iperf3` 服务实例。

```java
@Data
@Builder
public class IperfServerInstance {
    private String instanceId; // UUID
    private String remoteHost;
    private String boundIp;
    private int listeningPort;
    private int pid; // 进程ID
    private ServerStatus status; // e.g., RUNNING, STOPPED, ERROR

    public enum ServerStatus { RUNNING, STOPPED, ERROR }
}
```

### 3.3. `ClientTestConfig` (重构)
代表一次客户端测试的完整配置。

```java
@Data
@Builder
public class ClientTestConfig {
    private SshProfile sourceMachineProfile; // 发起测试的机器
    private String sourceBindAddress;      // 发起端绑定的IP
    private String targetHost;             // 目标服务端的IP
    private int targetPort;                // 目标服务端的端口
    private int duration;
    // ... 其他 iperf3 客户端参数
}
```

### 3.4. `ClientTestResult` (重构)
代表一次客户端测试的执行结果。

```java
@Data
@Builder
public class ClientTestResult {
    private String testId;
    private String testTimestamp;
    private ClientTestConfig configuration;
    private IperfResult clientJsonResult; // 仅包含客户端的iperf3 -J输出
    private boolean success;
    private String errorMessage;
}
```

---

## 4. 功能需求 (Functional Requirements)

### 4.1. 主界面 UI 布局 (全新设计)
应用程序主窗口将采用 **`JTabbedPane` (标签页)** 布局，包含三个核心功能区。

- **标签页一: "服务端管理" (`ServerManagementPanel`)**
    - **顶部:** 一个可复用的 `RemoteMachinePanel` 用于选择或新建SSH连接配置，并建立连接。
    - **中部:** “启动新服务”表单，包含输入字段：`绑定的IP` (下拉框，自动获取)、`监听端口`。旁边是“启动服务”按钮。
    - **底部:** 一个 `JTable`，用于实时展示所有已启动的 `IperfServerInstance`。表格列：`主机`, `监听IP`, `端口`, `PID`, `状态`。表格旁有“停止服务”和“强制终止”按钮。

- **标签页二: "客户端测试" (`ClientTestPanel`)**
    - **顶部:** 同样的 `RemoteMachinePanel` 用于连接将要发起测试的客户端机器。
    - **中部:** “测试参数”表单，包含输入字段：`目标IP`, `目标端口`, `时长`, `协议` 等。
    - **底部:** “开始测试”按钮，以及一个用于显示客户端测试结果的文本区域。

- **标签页三: "历史记录" (`HistoryPanel`)**
    - **顶部:** 提供筛选和搜索功能（例如按目标IP搜索）。
    - **主区域:** 一个 `JTable`，列出 `test_results` 目录下的所有历史测试。表格列：`测试时间`, `目标地址`, `平均速率`, `是否成功`。
    - **底部:** “查看详情”按钮，点击后可在弹窗或旁边的文本区域展示该条记录的完整JSON内容。

---

## 5. 核心工作流程 (Core Workflows)

### 5.1. 服务端管理流程
1.  用户在“服务端管理”标签页连接到一台远程主机。
2.  用户填写 `监听端口`，并从下拉框中选择 `绑定的IP`。
3.  用户点击“启动服务”。
4.  系统通过SSH在远程主机执行 `iperf3 -s -p [端口] -B [IP] -D` (以守护进程模式运行)，并记录返回的 `PID`。
5.  **端口占用处理:**
    - 如果启动命令失败并提示“地址已被占用”(Address already in use)，系统将自动执行 `ss -tlpn` 等命令来查找占用该端口的进程名和PID。
    - 系统弹出一个对话框，明确告知用户：“端口 [端口号] 已被进程 '[进程名]' (PID: [进程号]) 占用。是否需要强制终止该进程？”
    - 如果用户选择“是”，系统将执行 `kill -9 [进程号]`，然后重试启动`iperf3`服务。否则，操作取消。
6.  服务启动成功后，在界面下方的表格中新增一条 `IperfServerInstance` 记录，状态为 `RUNNING`。
7.  用户在表格中选中一个正在运行的服务：
    - 点击“停止服务”，系统执行 `kill [PID]` (发送 SIGTERM 信号)。
    - 点击“强制终止”，系统执行 `kill -9 [PID]` (发送 SIGKILL 信号)。
    - 进程结束后，更新表格中对应实例的状态为 `STOPPED`。

### 5.2. 客户端测试流程
1.  用户在“客户端测试”标签页连接到一台将要发起测试的源主机。
2.  用户填写所有测试参数（目标IP、端口、时长等）。
3.  用户点击“开始测试”。
4.  系统在源主机上执行 `iperf3 -c [目标IP] -p [端口] ... -J` 命令。
5.  测试完成后，将返回的JSON结果进行解析和格式化，显示在界面上。
6.  系统构建一个 `ClientTestResult` 对象，并使用 `Gson` 将其序列化为JSON文件，保存到 `test_results` 目录下。

### 5.3. 历史记录查看流程
1.  用户切换到“历史记录”标签页。
2.  系统自动扫描 `test_results` 目录，解析每个JSON文件名和内容，提取关键信息（时间、速率等）填充到表格中。
3.  用户在表格中选择一条记录并点击“查看详情”，程序将读取对应的JSON文件，并将其完整内容展示出来。

---

## 6. 非功能性需求 (Non-Functional Requirements)

- **日志记录:** 所有关键操作，包括但不限于：SSH连接/断开、执行的每一条远程命令、服务的启停、测试的开始与结束、发生的任何错误，都必须通过 SLF4J 进行详细记录。日志级别需合理划分 (INFO, WARN, ERROR, DEBUG)。
- **代码质量:**
    - **可读性:** 代码必须清晰、易于理解，并添加必要的注释（解释“为什么”，而不是“做什么”）。
    - **Lombok:**  적극적으로 Lombok annotation (@Data, @Builder, @Slf4j 등) 을 사용하여 Boilerplate 코드를 최소화한다.
- **错误处理:** 除了特定的端口占用场景外，所有可能发生的SSH异常、命令执行失败、文件IO异常等，都必须被捕获并以友好的方式（如弹窗提示）告知用户，同时记录详细的错误日志。

---

## 7. 未来扩展 (当前版本不实现)

- **实时图表:** 在客户端测试时，实时解析 `iperf3` 的中间输出，并绘制带宽曲线图。
- **批量测试:** 允许用户定义一个测试矩阵（如测试多个不同的包大小），程序自动排队执行。
- **SSH密钥认证:** 支持使用私钥文件进行SSH连接，提高安全性。
- **加密存储:** 对 `profiles.json` 中保存的密码进行加密。

---
