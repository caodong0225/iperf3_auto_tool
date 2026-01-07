package com.github.jyzxc.autoiperf.ui;

import com.github.jyzxc.autoiperf.controller.MainController;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private MainController mainController;
    @Getter
    private ConfigPanel configPanel;
    @Getter
    private ResultsPanel resultsPanel;
    @Getter
    private StatusBar statusBar;

    public MainFrame() {
        initComponents();
        // The controller will orchestrate actions between different panels
        mainController = new MainController(this);
    }

    private void initComponents() {
        setTitle("Auto iPerf3 - Network Performance Tester");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        configPanel = new ConfigPanel();
        resultsPanel = new ResultsPanel();
        statusBar = new StatusBar();

        setJMenuBar(createMenuBar());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configPanel, resultsPanel);
        splitPane.setDividerLocation(450);

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
