package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import java.awt.*;

/**
 * The main configuration panel on the left side of the application.
 * It holds two RemoteMachinePanel instances for client and server roles,
 * and a TestParametersPanel for common settings.
 */
public class ConfigPanel extends JPanel {

    public ConfigPanel() {
        setLayout(new BorderLayout(5, 5));
        initComponents();
    }

    private void initComponents() {
        // Main container for machine configs
        JSplitPane machineSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        machineSplitPane.setResizeWeight(0.5);
        machineSplitPane.setTopComponent(new RemoteMachinePanel("测试机 A (客户端角色)"));
        machineSplitPane.setBottomComponent(new RemoteMachinePanel("测试机 B (服务端角色)"));

        // Container for parameters and the main action button
        JPanel bottomPanel = new JPanel(new BorderLayout(5,5));
        bottomPanel.add(new TestParametersPanel(), BorderLayout.CENTER);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        actionPanel.add(new JButton("开始测试"));
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        // Add the two main sections to the panel
        add(machineSplitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}
