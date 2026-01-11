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
    private final JCheckBox bidirectionalCheckBox;
    private final JTextField targetIp2Field;
    private final JSpinner targetPort2Spinner;
    private final JComboBox<String> packetLengthComboBox;
    private final JTextField packetLengthCustomField;
    private final JSpinner intervalSpinner;
    private final JButton startTestButton;
    private final JTable clientProcessesTable;
    private final JButton refreshButton;
    private final JButton stopTestButton;
    private final JTextArea logArea;

    public ClientControlPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("客户端控制区"));
        
        // Part 1: Connection panel
        remoteMachinePanel = new RemoteMachinePanel("客户端主机");
        JScrollPane connectionScrollPane = new JScrollPane(remoteMachinePanel);
        connectionScrollPane.setBorder(new TitledBorder("连接配置"));
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

        // Row 4: Bidirectional test checkbox
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        bidirectionalCheckBox = new JCheckBox("是否双向测试");
        bidirectionalCheckBox.setToolTipText("启用后将同时启动发送和接收两个测试进程");
        parametersPanel.add(bidirectionalCheckBox, gbc);
        
        // Row 5: Target IP2 (only visible when bidirectional is checked)
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel targetIp2Label = new JLabel("目标IP2:");
        targetIp2Label.setEnabled(false);
        parametersPanel.add(targetIp2Label, gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 3; gbc.weightx = 1.0;
        targetIp2Field = new JTextField("");
        targetIp2Field.setToolTipText("双向测试的第二个目标IP（留空则使用目标IP1）");
        targetIp2Field.setEnabled(false);
        parametersPanel.add(targetIp2Field, gbc);
        
        // Row 6: Target Port2 (only visible when bidirectional is checked)
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel targetPort2Label = new JLabel("目标端口2:");
        targetPort2Label.setEnabled(false);
        parametersPanel.add(targetPort2Label, gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.gridwidth = 1; gbc.weightx = 0;
        targetPort2Spinner = new JSpinner(new SpinnerNumberModel(5201, 1, 65535, 1));
        targetPort2Spinner.setEnabled(false);
        parametersPanel.add(targetPort2Spinner, gbc);
        
        // Add listener to enable/disable bidirectional fields
        bidirectionalCheckBox.addActionListener(e -> {
            boolean enabled = bidirectionalCheckBox.isSelected();
            targetIp2Label.setEnabled(enabled);
            targetIp2Field.setEnabled(enabled);
            targetPort2Label.setEnabled(enabled);
            targetPort2Spinner.setEnabled(enabled);
        });

        // Row 7: Packet Length (-l parameter)
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        parametersPanel.add(new JLabel("发包大小:"), gbc);
        gbc.gridx = 1; gbc.gridy = 7; gbc.gridwidth = 1; gbc.weightx = 0;
        packetLengthComboBox = new JComboBox<>(new String[]{"64B", "128B", "256B", "1024B", "2048B", "4096B", "自定义"});
        packetLengthComboBox.setToolTipText("选择预设值或选择'自定义'后输入");
        parametersPanel.add(packetLengthComboBox, gbc);
        gbc.gridx = 2; gbc.gridy = 7; gbc.gridwidth = 1; gbc.weightx = 0;
        packetLengthCustomField = new JTextField("");
        packetLengthCustomField.setToolTipText("自定义发包大小（如：65536B）");
        packetLengthCustomField.setEnabled(false);
        parametersPanel.add(packetLengthCustomField, gbc);
        
        // Add listener to enable/disable custom field
        packetLengthComboBox.addActionListener(e -> {
            boolean enabled = "自定义".equals(packetLengthComboBox.getSelectedItem());
            packetLengthCustomField.setEnabled(enabled);
            if (enabled) {
                packetLengthCustomField.requestFocus();
            }
        });

        // Row 8: Interval (-i parameter)
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 1; gbc.weightx = 0;
        parametersPanel.add(new JLabel("记录间隔(秒):"), gbc);
        gbc.gridx = 1; gbc.gridy = 8; gbc.gridwidth = 1; gbc.weightx = 0;
        intervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));
        intervalSpinner.setToolTipText("每隔几秒记录一次数据（默认1秒）");
        parametersPanel.add(intervalSpinner, gbc);

        // Row 9: Start button
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 4; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        startTestButton = new JButton("开始新测试");
        startTestButton.setEnabled(false);
        parametersPanel.add(startTestButton, gbc);
        
        JScrollPane parametersScrollPane = new JScrollPane(parametersPanel);
        parametersScrollPane.setBorder(new TitledBorder("测试参数"));
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
        
        // Enable sorting for all columns
        clientProcessesTable.setAutoCreateRowSorter(true);
        
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
        tableScrollPane.setPreferredSize(new Dimension(600, 200));

        JPanel stopButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("刷新");
        stopTestButton = new JButton("终止测试");
        stopButtonPanel.add(refreshButton);
        stopButtonPanel.add(stopTestButton);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(tableScrollPane, BorderLayout.CENTER);
        bottomContainer.add(stopButtonPanel, BorderLayout.SOUTH);
        
        // Part 4: Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setText("客户端测试日志将显示在这里...\n");
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(new TitledBorder("客户端日志"));
        logScrollPane.setPreferredSize(new Dimension(500, 200));
        
        // Use JSplitPane for resizable sections
        // First split: Connection and Parameters
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, connectionScrollPane, parametersScrollPane);
        topSplitPane.setResizeWeight(0.5);
        topSplitPane.setDividerSize(5);
        topSplitPane.setOneTouchExpandable(true);
        topSplitPane.setContinuousLayout(true);
        SwingUtilities.invokeLater(() -> topSplitPane.setDividerLocation(0.55));
        
        // Second split: Top section (connection + parameters) and Table
        JSplitPane middleSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, bottomContainer);
        middleSplitPane.setResizeWeight(0.5);
        middleSplitPane.setDividerSize(5);
        middleSplitPane.setOneTouchExpandable(true);
        middleSplitPane.setContinuousLayout(true);
        SwingUtilities.invokeLater(() -> middleSplitPane.setDividerLocation(0.65));
        
        // Third split: Everything above and Log
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, middleSplitPane, logScrollPane);
        mainSplitPane.setResizeWeight(0.6);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setContinuousLayout(true);
        SwingUtilities.invokeLater(() -> mainSplitPane.setDividerLocation(0.6));

        add(mainSplitPane, BorderLayout.CENTER);
    }
}
