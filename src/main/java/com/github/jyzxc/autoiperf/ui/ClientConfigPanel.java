package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel for client-side configuration.
 */
public class ClientConfigPanel extends JPanel {
    public ClientConfigPanel() {
        setBorder(new TitledBorder("客户端配置"));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("本地网卡:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        add(new JComboBox<String>(new String[]{"自动检测中..."}), gbc);
    }
}
