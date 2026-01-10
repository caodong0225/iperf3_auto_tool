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

        // Use JSplitPane for resizable sections
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScrollPane, tableContainer);
        splitPane.setResizeWeight(0.4);
        splitPane.setDividerSize(5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.45));

        add(splitPane, BorderLayout.CENTER);
    }
}