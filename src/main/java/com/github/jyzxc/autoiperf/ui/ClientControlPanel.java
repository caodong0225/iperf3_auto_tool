package com.github.jyzxc.autoiperf.ui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

@Getter
public class ClientControlPanel extends JPanel {

    private final RemoteMachinePanel remoteMachinePanel;
    private final JTextField targetHostField;
    private final JSpinner targetPortSpinner;
    private final JSpinner durationSpinner;
    private final JComboBox<String> protocolComboBox;
    private final JButton startTestButton;
    private final JTable clientProcessesTable;
    private final JButton stopTestButton;

    public ClientControlPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("客户端控制区"));
        
        // Top part: Connection & Parameters
        JPanel topContainer = new JPanel(new BorderLayout());
        remoteMachinePanel = new RemoteMachinePanel("客户端主机");
        
        JPanel parametersPanel = new JPanel();
        parametersPanel.setLayout(new GridBagLayout());
        parametersPanel.setBorder(new TitledBorder("测试参数"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; parametersPanel.add(new JLabel("目标主机:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; targetHostField = new JTextField("127.0.0.1"); parametersPanel.add(targetHostField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; parametersPanel.add(new JLabel("目标端口:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; targetPortSpinner = new JSpinner(new SpinnerNumberModel(5201, 1, 65535, 1)); parametersPanel.add(targetPortSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 1; parametersPanel.add(new JLabel("时长(秒):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; durationSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1)); parametersPanel.add(durationSpinner, gbc);

        gbc.gridx = 2; gbc.gridy = 1; parametersPanel.add(new JLabel("协议:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; protocolComboBox = new JComboBox<>(new String[]{"TCP", "UDP"}); parametersPanel.add(protocolComboBox, gbc);

        JPanel startButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        startTestButton = new JButton("开始新测试");
        startButtonPanel.add(startTestButton);
        
        topContainer.add(remoteMachinePanel, BorderLayout.NORTH);
        topContainer.add(parametersPanel, BorderLayout.CENTER);
        topContainer.add(startButtonPanel, BorderLayout.SOUTH);

        // Bottom part: Table of running/recent tests
        String[] columnNames = {"测试ID", "目标", "状态", "结果"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        clientProcessesTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(clientProcessesTable);
        tableScrollPane.setBorder(new TitledBorder("活动/近期测试"));

        JPanel stopButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stopTestButton = new JButton("终止测试");
        stopButtonPanel.add(stopTestButton);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(tableScrollPane, BorderLayout.CENTER);
        bottomContainer.add(stopButtonPanel, BorderLayout.SOUTH);
        
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topContainer, bottomContainer);
        mainSplitPane.setResizeWeight(0.4);

        add(mainSplitPane, BorderLayout.CENTER);
    }
}
