package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel for general iperf3 test parameters.
 */
public class TestParametersPanel extends JPanel {
    private JSpinner portSpinner;
    private JComboBox<String> protocolComboBox;
    private JSpinner durationSpinner;
    private JComboBox<String> packetSizeComboBox;

    public TestParametersPanel() {
        setBorder(new TitledBorder("通用测试参数"));
        setLayout(new GridBagLayout());
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 0: Port & Protocol
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("端口:"), gbc);
        gbc.gridx = 1;
        portSpinner = new JSpinner(new SpinnerNumberModel(5201, 1, 65535, 1));
        add(portSpinner, gbc);
        
        gbc.gridx = 2; add(new JLabel("协议:"), gbc);
        gbc.gridx = 3;
        protocolComboBox = new JComboBox<>(new String[]{"TCP", "UDP"});
        add(protocolComboBox, gbc);

        // Row 1: Duration & Packet Size
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("时长 (秒):"), gbc);
        gbc.gridx = 1;
        durationSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        add(durationSpinner, gbc);
        
        gbc.gridx = 2; add(new JLabel("发包大小:"), gbc);
        gbc.gridx = 3;
        String[] packetSizes = {"64B", "128B", "256B", "512B", "1024B", "2048B", "4096B", "8192B"};
        packetSizeComboBox = new JComboBox<>(packetSizes);
        add(packetSizeComboBox, gbc);
    }

    // Getters for controller
    public JSpinner getPortSpinner() { return portSpinner; }
    public JComboBox<String> getProtocolComboBox() { return protocolComboBox; }
    public JSpinner getDurationSpinner() { return durationSpinner; }
    public JComboBox<String> getPacketSizeComboBox() { return packetSizeComboBox; }
}
