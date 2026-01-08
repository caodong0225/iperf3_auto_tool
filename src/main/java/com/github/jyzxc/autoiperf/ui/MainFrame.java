package com.github.jyzxc.autoiperf.ui;

import com.github.jyzxc.autoiperf.controller.ServerControlController;
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

    // Controllers
    private ServerControlController serverControlController;


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

        // 2. Instantiate Main Panels
        configPanel = new ConfigPanel();
        resultsPanel = new ResultsPanel();
        statusBar = new StatusBar();
        
        // 3. Instantiate Controllers and link them to Views and Services
        serverControlController = new ServerControlController(configPanel.getServerControlPanel(), serverManagerService);
        // Other controllers will be initialized here later

        setJMenuBar(createMenuBar());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configPanel, resultsPanel);
        splitPane.setDividerLocation(550);

        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
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
