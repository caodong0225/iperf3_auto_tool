package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import java.awt.*;

/**
 * The status bar at the bottom of the application window.
 */
public class StatusBar extends JPanel {

    private JLabel statusLabel;

    public StatusBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("准备就绪");
        add(statusLabel);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}
