package com.github.jyzxc.autoiperf.ui;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * The main configuration panel on the left side of the application.
 * It holds the server and client control panels.
 */
@Getter
public class ConfigPanel extends JPanel {

    private ServerControlPanel serverControlPanel;
    private ClientControlPanel clientControlPanel;

    public ConfigPanel() {
        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.5);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setContinuousLayout(true);

        // The top part for Server Management
        serverControlPanel = new ServerControlPanel();

        // The bottom part for Client Management
        clientControlPanel = new ClientControlPanel();

        mainSplitPane.setTopComponent(serverControlPanel);
        mainSplitPane.setBottomComponent(clientControlPanel);
        
        // Set initial divider location to give both panels equal space
        // This will be set after the frame is visible
        SwingUtilities.invokeLater(() -> {
            int totalHeight = mainSplitPane.getHeight();
            if (totalHeight > 0) {
                mainSplitPane.setDividerLocation(totalHeight / 2);
            }
        });

        add(mainSplitPane, BorderLayout.CENTER);
    }
}