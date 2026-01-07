package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import java.awt.*;

/**
 * The main configuration panel on the left side of the application.
 * It holds client, server, and test parameter configuration panels.
 */
public class ConfigPanel extends JPanel {

    public ConfigPanel() {
        setLayout(new BorderLayout(5, 5));
        initComponents();
    }

    private void initComponents() {
        JPanel serverAndClientPanel = new JPanel();
        serverAndClientPanel.setLayout(new GridLayout(2, 1, 5, 5));
        serverAndClientPanel.add(new ClientConfigPanel());
        serverAndClientPanel.add(new ServerConfigPanel());

        add(serverAndClientPanel, BorderLayout.NORTH);
        add(new TestParametersPanel(), BorderLayout.CENTER);

        // Panel for action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionPanel.add(new JButton("开始测试"));
        add(actionPanel, BorderLayout.SOUTH);
    }
}
