package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel for general iperf3 test parameters.
 */
public class TestParametersPanel extends JPanel {
    public TestParametersPanel() {
        setBorder(new TitledBorder("测试参数"));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Port & Protocol
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("端口:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        add(new JSpinner(new SpinnerNumberModel(5201, 1, 65535, 1)), gbc);
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        add(new JLabel("协议:"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        add(new JComboBox<String>(new String[]{"TCP", "UDP"}), gbc);


        // Row 1: Duration
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("时长 (秒):"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        add(new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1)), gbc);
        
        // Add more parameters here in the future
    }
}
