package com.github.jyzxc.autoiperf.ui;

import com.github.jyzxc.autoiperf.controller.ClientControlController;
import com.github.jyzxc.autoiperf.controller.FileManagementController;
import com.github.jyzxc.autoiperf.controller.ServerControlController;
import com.github.jyzxc.autoiperf.service.ClientManagerService;
import com.github.jyzxc.autoiperf.service.ClientTestService;
import com.github.jyzxc.autoiperf.service.FileManagementService;
import com.github.jyzxc.autoiperf.service.ServerManagerService;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    @Getter
    private ServerControlPanel serverControlPanel;
    @Getter
    private ClientControlPanel clientControlPanel;
    @Getter
    private StatusBar statusBar;

    // Services
    private ServerManagerService serverManagerService;
    private ClientTestService clientTestService;
    private ClientManagerService clientManagerService;
    private FileManagementService fileManagementService;

    // Controllers
    private ServerControlController serverControlController;
    private ClientControlController clientControlController;
    private FileManagementController fileManagementController;


    public MainFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Auto iPerf3 - Management & Testing Tool");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 1. Instantiate Core Services
        serverManagerService = new ServerManagerService();
        clientTestService = new ClientTestService();
        clientManagerService = new ClientManagerService();
        fileManagementService = new FileManagementService();

        // 2. Instantiate Main Panels
        serverControlPanel = new ServerControlPanel();
        clientControlPanel = new ClientControlPanel();
        statusBar = new StatusBar();
        FileManagementPanel fileManagementPanel = new FileManagementPanel();
        
        // 3. Instantiate Controllers and link them to Views and Services
        serverControlController = new ServerControlController(
                serverControlPanel, 
                serverManagerService,
                null); // No longer using ResultsPanel
        
        clientControlController = new ClientControlController(
                clientControlPanel,
                clientManagerService,
                null, // No longer using ResultsPanel
                serverControlPanel);
        
        fileManagementController = new FileManagementController(
                fileManagementPanel,
                fileManagementService);

        setJMenuBar(createMenuBar());

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Tab 1: Client (left) and Server (right) side by side
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientControlPanel, serverControlPanel);
        mainSplitPane.setDividerLocation(500);
        mainSplitPane.setResizeWeight(0.5);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setDividerSize(8);
        tabbedPane.addTab("服务管理", mainSplitPane);
        
        // Tab 2: File Management
        tabbedPane.addTab("文件管理", fileManagementPanel);

        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        
        // After frame is visible, ensure proper layout
        SwingUtilities.invokeLater(() -> {
            validate();
            repaint();
            // Force panels to layout properly
            if (clientControlPanel != null) {
                clientControlPanel.validate();
                clientControlPanel.repaint();
            }
            if (serverControlPanel != null) {
                serverControlPanel.validate();
                serverControlPanel.repaint();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);
        return menuBar;
    }
    
    private void showAboutDialog() {
        String aboutText = 
            "Auto iPerf3 - 管理与测试工具\n\n" +
            "版本: 1.0\n\n" +
            "项目概述:\n" +
            "本软件是一款基于 Java Swing 的 iperf3 管理与测试工具，将服务端管理与客户端测试功能完全解耦，\n" +
            "为用户提供更大的灵活性。\n\n" +
            "核心功能:\n" +
            "1. 服务端管理器: 允许用户在任意远程主机上独立启动、监控和停止 iperf3 服务进程。\n" +
            "2. 客户端测试器: 允许用户从任意远程主机发起 iperf3 客户端测试，可连接到网络上任何一个 iperf3 服务端。\n" +
            "3. 文件管理: 管理远程主机上的 iperf3 JSON 测试结果文件，支持查看性能指标、下载和删除。\n\n" +
            "技术栈:\n" +
            "- 核心语言: JDK 17\n" +
            "- 图形界面: Swing, FlatLaf\n" +
            "- SSH通信: JSch\n" +
            "- JSON处理: Gson\n" +
            "- 日志框架: SLF4J / Logback\n\n" +
            "主要特性:\n" +
            "- 支持 TCP 和 UDP 协议测试\n" +
            "- 支持双向测试（同时进行发送和接收测试）\n" +
            "- 可自定义发包大小和记录间隔\n" +
            "- 自动发现和管理运行中的 iperf3 进程\n" +
            "- 实时显示测试速率和 CPU 使用率\n" +
            "- 支持网卡 IP 地址自定义命名\n" +
            "- 增量刷新机制，提高性能\n\n" +
            "使用说明:\n" +
            "1. 服务端管理: 在右侧面板连接远程主机，选择网卡 IP 和端口，点击\"开启新服务\"启动 iperf3 服务端。\n" +
            "2. 客户端测试: 在左侧面板连接远程主机，配置测试参数（目标 IP、端口、时长等），点击\"开始新测试\"。\n" +
            "3. 文件管理: 在\"文件管理\"标签页连接远程主机，查看、下载或删除测试结果文件。\n\n" +
            "注意事项:\n" +
            "- 确保远程主机已安装 iperf3\n" +
            "- 确保 SSH 连接正常\n" +
            "- 测试结果文件保存在远程主机的 /tmp/iperf3 目录下";
        
        JTextArea textArea = new JTextArea(aboutText);
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(600, 500));
        
        JOptionPane.showMessageDialog(this, scrollPane, "关于 Auto iPerf3", JOptionPane.INFORMATION_MESSAGE);
    }
}
