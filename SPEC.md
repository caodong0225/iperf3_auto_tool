# 软件需求规格说明书 (Software Requirements Specification)

**版本: 2.0**

**日期: 2026-01-07**

---

## 1. 项目概述

### 1.1. 项目目标

本项目旨在开发一款基于 Java Swing 的**`iperf3` 测试编排控制器**。该软件作为一个纯粹的**控制器 (Controller)**，通过 SSH 同时连接并管理两台远程 Linux 测试机，分别令其扮演 `iperf3` 的客户端和服务端角色，从而完成网络性能测试。软件提供图形化界面进行配置、触发和结果展示。

### 1.2. 核心角色定义

- **GUI 应用 (控制器):** 用户在本机运行的 `.jar` 程序。它不参与网络测试，仅负责远程控制。
- **测试机 A (客户端角色):** 一台远程 Linux 服务器，由用户提供 SSH凭证。控制器将登录该机器并执行 `iperf3 -c` 命令。
- **测试机 B (服务端角色):** 另一台远程 Linux 服务器，由用户提供 SSH凭证。控制器将登录该机器并执行 `iperf3 -s` 命令。

---

## 2. 技术栈与架构

(内容无重大变化)

- **技术选型:** JDK 17, Swing, FlatLaf, JSch, Lombok, SLF4J/Logback.
- **架构设计原则:** 单例、工厂、责任链模式依然适用，但其应用对象将适配新的双SSH控制模型。

---

## 3. 核心数据模型 (Data Models)

### 3.1. `SshProfile`
代表一个远程测试机的连接配置 (替换原 `ServerProfile`)。

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SshProfile {
    private String profileName; // e.g., "北京机房-客户端"
    private String host;
    private String username;
    private String password;
}
```

### 3.2. `TestConfig`
包含两台测试机的配置和通用测试参数。

```java
@Data
@Builder
public class TestConfig {
    private SshProfile clientMachineProfile;
    private SshProfile serverMachineProfile;
    private String clientBindAddress; // 测试机A上选定的IP
    private String serverBindAddress; // 测试机B上选定的IP
    private int testPort;
    private int duration;
    private String packetSize;
    // ... 其他通用 iperf3 参数
}
```

### 3.3. `TestResult`
包含两台测试机的独立结果。

```java
@Data
@Builder
public class TestResult {
    // ... testId, timestamp, configuration ...
    private IperfJsonResult clientResult; // 测试机A的结果
    private IperfJsonResult serverResult; // 测试机B的结果
    // ... success, errorMessage ...

    // 直接映射 iperf3 -J 输出的 JSON 结构
    @Data
    public static class IperfJsonResult {
        private /* iperf3 JSON 结构对应的所有字段 */ ;
    }
}
```

---

## 4. 功能需求 (Functional Requirements)

### 4.1. 主界面 UI 布局 (全新设计)

- **主窗口 (`JFrame`):** 整体布局不变。
- **左侧配置面板 (`ConfigPanel`):**
    - **采用上下布局 (`JSplitPane`)。**
    - **上半区: “测试机 A (客户端角色)” 配置面板。**
    - **下半区: “测试机 B (服务端角色)” 配置面板。**
    - 两个区域的**布局和组件完全镜像对称**，均包含：
        - `JComboBox` 用于选择和管理已保存的 SSH 历史配置。
        - `JTextField` 用于输入主机地址、用户名。
        - `JPasswordField` 用于输入密码。
        - `JPanel` 包含 “连接”、“保存”、“删除” 按钮。
        - `JLabel` 和 `JComboBox` 用于在连接成功后，显示和选择本机的网卡IP地址。
- **中央区域 (`TestParametersPanel`):**
    - 位于左右两个主面板之间或下方，用于放置**通用测试参数**，如：测试端口、时长、协议、发包大小。
- **右侧结果面板 (`ResultsPanel`):**
    - 保持上下分裂布局，分别用于显示“测试机 A (客户端)”和“测试机 B (服务端)”的测试结果。
- **底部:** "开始测试" 按钮和状态栏。

### 4.2. 核心工作流程

1.  用户分别为“测试机A”和“测试机B”填入SSH信息，并各自点击“连接”。
2.  程序独立地与两台机器建立SSH连接。
3.  对于每台连接成功的机器，程序自动获取其网卡IP列表，并填充到其专属的网卡下拉框中。
4.  当**两台机器都连接成功**后，“开始测试”按钮变为可用。
5.  用户配置通用参数，并为两台机器选定要绑定的IP。
6.  用户点击“开始测试”，触发测试责任链：
    a. **服务端准备:** 通过SSH在**测试机B**上执行 `iperf3 -s -p [端口] -B [IP-B] -J > /tmp/iperf_server.json` 命令，并获取其PID。
    b. **客户端执行:** 通过SSH在**测试机A**上执行 `iperf3 -c [IP-B] -p [端口] -B [IP-A] ... -J` 命令，并等待其完成。
    c. **结果收集:**
        - 直接捕获测试机A上命令的标准输出（JSON内容）。
        - 通过SSH在测试机B上执行 `cat /tmp/iperf_server.json` 来获取服务端的结果。
    d. **结果展示:** 将两份JSON结果格式化后，分别显示在右侧的两个结果区域。
    e. **持久化:** 将包含两份结果的 `TestResult` 对象存入JSON文件。
    f. **清理:**
        - 通过SSH在测试机B上执行 `kill [PID]`，并 `rm /tmp/iperf_server.json`。
        - 程序退出时，通过Shutdown Hook尽力完成清理。

---

## 5. 非功能性需求
(错误处理、日志、交付物等策略保持不变，但将应用于双SSH场景)

---

## 6. 未来扩展 (当前版本不实现)

- **命令行工具 (CLI) 模式。**
- SSH 公钥认证。
- 历史测试报告的统计与可视化分析。
- `iperf3` CPU 占用率统计。

---
