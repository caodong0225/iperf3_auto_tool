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

        // The top part for Server Management
        serverControlPanel = new ServerControlPanel();

        // The bottom part for Client Management
        clientControlPanel = new ClientControlPanel();

        mainSplitPane.setTopComponent(serverControlPanel);
        mainSplitPane.setBottomComponent(clientControlPanel);

        add(mainSplitPane, BorderLayout.CENTER);
    }
}