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
        JMenu fileMenu = new JMenu("文件");
        JMenuItem openResultsDir = new JMenuItem("打开结果目录");
        fileMenu.add(openResultsDir);

        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }
}
