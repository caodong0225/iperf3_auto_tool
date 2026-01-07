package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel on the right to display test results, split into server and client sections.
 */
public class ResultsPanel extends JPanel {
    private JTextArea serverArea;
    private JTextArea clientArea;

    public ResultsPanel() {
        setLayout(new BorderLayout());
        
        serverArea = createTextArea("测试机 B (服务端) 的结果将显示在这里...");
        JScrollPane serverScrollPane = new JScrollPane(serverArea);
        serverScrollPane.setBorder(new TitledBorder("测试机 B (服务端) 结果"));

        clientArea = createTextArea("测试机 A (客户端) 的结果将显示在这里...");
        JScrollPane clientScrollPane = new JScrollPane(clientArea);
        clientScrollPane.setBorder(new TitledBorder("测试机 A (客户端) 结果"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, clientScrollPane, serverScrollPane);
        splitPane.setResizeWeight(0.5); 

        add(splitPane, BorderLayout.CENTER);
    }

    private JTextArea createTextArea(String initialText) {
        JTextArea textArea = new JTextArea(initialText);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setMargin(new Insets(5, 5, 5, 5));
        return textArea;
    }

    public void setClientResultText(String text) {
        clientArea.setText(text);
        clientArea.setCaretPosition(0); // Scroll to top
    }

    public void setServerResultText(String text) {
        serverArea.setText(text);
        serverArea.setCaretPosition(0); // Scroll to top
    }
}
