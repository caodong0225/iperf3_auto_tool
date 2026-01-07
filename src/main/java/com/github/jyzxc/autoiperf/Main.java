package com.github.jyzxc.autoiperf;

import com.formdev.flatlaf.FlatLightLaf;
import com.github.jyzxc.autoiperf.ui.MainFrame;

import javax.swing.*;

/**
 * Main application entry point.
 */
public class Main {

    public static void main(String[] args) {
        // Setup modern look and feel
        FlatLightLaf.setup();

        // Start the Swing GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
