package com.github.jyzxc.autoiperf.ui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

@Getter
public class ClientControlPanel extends JPanel {

    private final RemoteMachinePanel remoteMachinePanel;
    private final JTextField targetIpField;
    private final JSpinner targetPortSpinner;
    private final JSpinner durationSpinner;
    private final JComboBox<String> protocolComboBox;
    private final JButton startTestButton;
    private final JTable clientProcessesTable;
    private final JButton stopTestButton;

    public ClientControlPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("客户端控制区"));
        
        // Part 1: Connection panel
        remoteMachinePanel = new RemoteMachinePanel("客户端主机");
        JScrollPane connectionScrollPane = new JScrollPane(remoteMachinePanel);
        connectionScrollPane.setBorder(new TitledBorder("连接配置"));
        connectionScrollPane.setMinimumSize(new Dimension(400, 180));
        connectionScrollPane.setPreferredSize(new Dimension(500, 220));
        
        // Part 2: Test parameters panel
        JPanel parametersPanel = new JPanel(new GridBagLayout());
        parametersPanel.setBorder(new TitledBorder("测试参数"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Target IP
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        parametersPanel.add(new JLabel("目标IP:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        targetIpField = new JTextField("");
        targetIpField.setToolTipText("从上方服务端表格选择，或手动输入");
        parametersPanel.add(targetIpField, gbc);
        
        // Row 1: Target Port
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        parametersPanel.add(new JLabel("目标端口:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        targetPortSpinner = new JSpinner(new SpinnerNumberModel(5201, 1, 65535, 1));
        parametersPanel.add(targetPortSpinner, gbc);

        // Row 2: Duration
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        parametersPanel.add(new JLabel("时长(秒):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        durationSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        parametersPanel.add(durationSpinner, gbc);

        // Row 3: Protocol
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        parametersPanel.add(new JLabel("协议:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        protocolComboBox = new JComboBox<>(new String[]{"TCP", "UDP"});
        parametersPanel.add(protocolComboBox, gbc);

        // Row 4: Start button
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        startTestButton = new JButton("开始新测试");
        startTestButton.setEnabled(false);
        parametersPanel.add(startTestButton, gbc);
        
        JScrollPane parametersScrollPane = new JScrollPane(parametersPanel);
        parametersScrollPane.setBorder(new TitledBorder("测试参数"));
        parametersScrollPane.setMinimumSize(new Dimension(400, 180));
        parametersScrollPane.setPreferredSize(new Dimension(500, 200));

        // Part 3: Table of running/recent tests
        String[] columnNames = {"PID", "状态", "目标IP", "目标端口", "完整命令"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clientProcessesTable = new JTable(tableModel);
        
        // Configure table column widths
        clientProcessesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        clientProcessesTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // PID
        clientProcessesTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // 状态
        clientProcessesTable.getColumnModel().getColumn(2).setPreferredWidth(120); // 目标IP
        clientProcessesTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // 目标端口
        clientProcessesTable.getColumnModel().getColumn(4).setPreferredWidth(300); // 完整命令
        
        clientProcessesTable.setPreferredScrollableViewportSize(new Dimension(600, 150));
        clientProcessesTable.setFillsViewportHeight(true);
        
        JScrollPane tableScrollPane = new JScrollPane(clientProcessesTable);
        tableScrollPane.setBorder(new TitledBorder("活动的客户端测试"));
        tableScrollPane.setMinimumSize(new Dimension(400, 150));
        tableScrollPane.setPreferredSize(new Dimension(600, 200));

        JPanel stopButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stopTestButton = new JButton("终止测试");
        stopButtonPanel.add(stopTestButton);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(tableScrollPane, BorderLayout.CENTER);
        bottomContainer.add(stopButtonPanel, BorderLayout.SOUTH);
        
        // Use JSplitPane for resizable sections
        // First split: Connection and Parameters
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, connectionScrollPane, parametersScrollPane);
        topSplitPane.setResizeWeight(0.5);
        topSplitPane.setDividerSize(5);
        topSplitPane.setOneTouchExpandable(true);
        topSplitPane.setMinimumSize(new Dimension(400, 200));
        
        // Second split: Top section and Table
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, bottomContainer);
        mainSplitPane.setResizeWeight(0.5);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setMinimumSize(new Dimension(400, 300));

        add(mainSplitPane, BorderLayout.CENTER);
    }
}