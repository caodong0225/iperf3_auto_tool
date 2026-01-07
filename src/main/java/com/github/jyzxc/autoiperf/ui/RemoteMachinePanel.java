package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * A reusable panel for configuring a single remote test machine (either client or server role).
 */
public class RemoteMachinePanel extends JPanel {

    public RemoteMachinePanel(String title) {
        setBorder(new TitledBorder(title));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Saved Profiles
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("历史配置:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        add(new JComboBox<String>(new String[]{"新建连接..."}), gbc);

        // Row 1: Host
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("主机地址:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(new JTextField(""), gbc);

        // Row 2: Username
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        add(new JTextField("root"), gbc);

        // Row 3: Password
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("密码:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        add(new JPasswordField(""), gbc);

        // Row 4: Action Buttons
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(new JButton("连接"));
        buttonPanel.add(new JButton("保存"));
        buttonPanel.add(new JButton("删除"));
        add(buttonPanel, gbc);

        // Row 5: NIC Selection
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel("网卡 IP:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JComboBox<String> nicComboBox = new JComboBox<>(new String[]{"待连接..."});
        nicComboBox.setEnabled(false); // Disabled until connected
        add(nicComboBox, gbc);
    }
}
