package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel on the right to display test results, split into server and client sections.
 */
public class ResultsPanel extends JPanel {
    public ResultsPanel() {
        // Use a BorderLayout to contain the main split pane
        setLayout(new BorderLayout());
        
        // Server Results Area
        JTextArea serverArea = new JTextArea("服务端测试结果将显示在这里...");
        serverArea.setEditable(false);
        serverArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        serverArea.setMargin(new Insets(5,5,5,5));
        JScrollPane serverScrollPane = new JScrollPane(serverArea);
        serverScrollPane.setBorder(new TitledBorder("服务端结果"));

        // Client Results Area
        JTextArea clientArea = new JTextArea("客户端测试结果将显示在这里...");
        clientArea.setEditable(false);
        clientArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        clientArea.setMargin(new Insets(5,5,5,5));
        JScrollPane clientScrollPane = new JScrollPane(clientArea);
        clientScrollPane.setBorder(new TitledBorder("客户端结果"));

        // Vertical Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, serverScrollPane, clientScrollPane);
        splitPane.setResizeWeight(0.5); // Divide space equally

        add(splitPane, BorderLayout.CENTER);
    }
}
