package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;

/**
 * The main window frame of the application.
 */
public class MainFrame extends JFrame {

    public MainFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Auto iPerf3 - Network Performance Tester");
        setSize(800, 600);
        setLocationRelativeTo(null); // Center the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
