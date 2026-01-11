package com.github.jyzxc.autoiperf.ui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

@Getter
public class ServerControlPanel extends JPanel {

    private final RemoteMachinePanel remoteMachinePanel;
    private final JSpinner portSpinner;
    private final JButton startServerButton;
    private final JTable serverInstancesTable;
    private final JButton refreshButton;
    private final JButton stopServerButton;
    private final JButton killServerButton;
    private final JTextArea logArea;

    public ServerControlPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("服务端控制区"));
        
        // Top part: Connection and Start controls
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        remoteMachinePanel = new RemoteMachinePanel("服务端主机");
        remoteMachinePanel.setPreferredSize(new Dimension(520, 220));
        topPanel.add(remoteMachinePanel, BorderLayout.CENTER);

        JPanel startControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startControlsPanel.add(new JLabel("监听端口:"));
        portSpinner = new JSpinner(new SpinnerNumberModel(5201, 1, 65535, 1));
        startControlsPanel.add(portSpinner);
        startServerButton = new JButton("开启新服务");
        startControlsPanel.add(startServerButton);
        topPanel.add(startControlsPanel, BorderLayout.SOUTH);
        JScrollPane topScrollPane = new JScrollPane(topPanel);
        topScrollPane.setBorder(new TitledBorder("连接与启动"));
        topScrollPane.setPreferredSize(new Dimension(600, 280));

        // Center part: Table of running instances
        String[] columnNames = {"PID", "状态", "监听IP", "端口", "完整命令"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serverInstancesTable = new JTable(tableModel);
        
        // Enable sorting for all columns
        serverInstancesTable.setAutoCreateRowSorter(true);
        
        // Configure table column widths
        serverInstancesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        serverInstancesTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // PID
        serverInstancesTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // 状态
        serverInstancesTable.getColumnModel().getColumn(2).setPreferredWidth(120); // 监听IP
        serverInstancesTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // 端口
        serverInstancesTable.getColumnModel().getColumn(4).setPreferredWidth(300); // 完整命令
        
        // Set table preferred size to ensure visibility
        serverInstancesTable.setPreferredScrollableViewportSize(new Dimension(600, 150));
        serverInstancesTable.setFillsViewportHeight(true);
        
        JScrollPane tableScrollPane = new JScrollPane(serverInstancesTable);
        tableScrollPane.setBorder(new TitledBorder("活动的服务实例"));
        // Set minimum and preferred sizes to ensure table is visible
        tableScrollPane.setMinimumSize(new Dimension(400, 150));
        tableScrollPane.setPreferredSize(new Dimension(600, 250));
        
        // Bottom part: Refresh, Stop/Kill controls for selected instance
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("刷新");
        stopServerButton = new JButton("停止服务");
        killServerButton = new JButton("强制终止");
        bottomPanel.add(refreshButton);
        bottomPanel.add(stopServerButton);
        bottomPanel.add(killServerButton);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.add(tableScrollPane, BorderLayout.CENTER);
        tableContainer.add(bottomPanel, BorderLayout.SOUTH);

        // Part 4: Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setText("服务端操作日志将显示在这里...\n");
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(new TitledBorder("服务端日志"));
        logScrollPane.setPreferredSize(new Dimension(600, 200));

        // Use JSplitPane for resizable sections
        // First split: Top section (connection + table) and Log
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setContinuousLayout(true);
        
        // Second split: Connection and Table
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScrollPane, tableContainer);
        topSplitPane.setResizeWeight(0.4);
        topSplitPane.setDividerSize(5);
        topSplitPane.setOneTouchExpandable(true);
        topSplitPane.setContinuousLayout(true);
        SwingUtilities.invokeLater(() -> topSplitPane.setDividerLocation(0.45));
        
        mainSplitPane.setTopComponent(topSplitPane);
        mainSplitPane.setBottomComponent(logScrollPane);
        SwingUtilities.invokeLater(() -> mainSplitPane.setDividerLocation(0.6));

        add(mainSplitPane, BorderLayout.CENTER);
    }
}