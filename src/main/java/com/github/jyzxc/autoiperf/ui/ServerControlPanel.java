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
    private final JButton stopServerButton;
    private final JButton killServerButton;

    public ServerControlPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("服务端控制区"));
        
        // Top part: Connection and Start controls
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        remoteMachinePanel = new RemoteMachinePanel("服务端主机");
        topPanel.add(remoteMachinePanel, BorderLayout.CENTER);

        JPanel startControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startControlsPanel.add(new JLabel("监听端口:"));
        portSpinner = new JSpinner(new SpinnerNumberModel(5201, 1, 65535, 1));
        startControlsPanel.add(portSpinner);
        startServerButton = new JButton("开启新服务");
        startControlsPanel.add(startServerButton);
        topPanel.add(startControlsPanel, BorderLayout.SOUTH);

        // Center part: Table of running instances
        String[] columnNames = {"PID", "状态", "监听IP", "端口", "完整命令"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serverInstancesTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(serverInstancesTable);
        tableScrollPane.setBorder(new TitledBorder("活动的服务实例"));

        // Bottom part: Stop/Kill controls for selected instance
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stopServerButton = new JButton("停止服务");
        killServerButton = new JButton("强制终止");
        bottomPanel.add(stopServerButton);
        bottomPanel.add(killServerButton);

        add(topPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}