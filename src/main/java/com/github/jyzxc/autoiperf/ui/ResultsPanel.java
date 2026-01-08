package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Panel on the right to display various results and logs.
 */
public class ResultsPanel extends JPanel {
    
    private final JTextArea logArea;

    public ResultsPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("结果与日志"));
        
        logArea = new JTextArea("程序日志和测试结果将显示在这里...");
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendLog(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto-scroll
    }

    public void setLog(String text) {
        logArea.setText(text);
        logArea.setCaretPosition(0);
    }
}