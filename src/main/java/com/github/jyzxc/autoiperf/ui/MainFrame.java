package com.github.jyzxc.autoiperf.ui;

import com.github.jyzxc.autoiperf.controller.ClientControlController;
import com.github.jyzxc.autoiperf.controller.ServerControlController;
import com.github.jyzxc.autoiperf.service.ClientManagerService;
import com.github.jyzxc.autoiperf.service.ClientTestService;
import com.github.jyzxc.autoiperf.service.ServerManagerService;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    @Getter
    private ConfigPanel configPanel;
    @Getter
    private ResultsPanel resultsPanel;
    @Getter
    private StatusBar statusBar;

    // Services
    private ServerManagerService serverManagerService;
    private ClientTestService clientTestService;
    private ClientManagerService clientManagerService;

    // Controllers
    private ServerControlController serverControlController;
    private ClientControlController clientControlController;


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

        // 2. Instantiate Main Panels
        configPanel = new ConfigPanel();
        resultsPanel = new ResultsPanel();
        statusBar = new StatusBar();
        
        // 3. Instantiate Controllers and link them to Views and Services
        serverControlController = new ServerControlController(
                configPanel.getServerControlPanel(), 
                serverManagerService,
                resultsPanel);
        
        clientControlController = new ClientControlController(
                configPanel.getClientControlPanel(),
                clientTestService,
                clientManagerService,
                resultsPanel,
                configPanel.getServerControlPanel());

        setJMenuBar(createMenuBar());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configPanel, resultsPanel);
        splitPane.setDividerLocation(550);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        
        // After frame is visible, ensure proper layout
        SwingUtilities.invokeLater(() -> {
            validate();
            repaint();
            // Force ConfigPanel's internal split pane to layout properly
            if (configPanel != null && configPanel.getServerControlPanel() != null) {
                configPanel.getServerControlPanel().validate();
                configPanel.getServerControlPanel().repaint();
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
