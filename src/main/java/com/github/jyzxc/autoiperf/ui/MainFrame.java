package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import java.awt.*;

/**
 * The main window frame of the application.
 */
public class MainFrame extends JFrame {

    private JMenuBar menuBar;
    private ConfigPanel configPanel;
    private ResultsPanel resultsPanel;
    private StatusBar statusBar;

    public MainFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Auto iPerf3 - Network Performance Tester");
        setSize(1024, 768);
        setLocationRelativeTo(null); // Center the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create Panels
        configPanel = new ConfigPanel();
        resultsPanel = new ResultsPanel();
        statusBar = new StatusBar();

        // Setup Menu Bar
        menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Setup Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configPanel, resultsPanel);
        splitPane.setDividerLocation(350); // Give config panel a fixed initial width

        // Add components to the frame
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
