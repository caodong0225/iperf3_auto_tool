package com.github.jyzxc.autoiperf.ui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

@Getter
public class ResultsPanel extends JPanel {

    private final JTextArea serverLogArea;
    private final JTextArea clientResultArea;

    public ResultsPanel() {
        setLayout(new BorderLayout());
        
        serverLogArea = new JTextArea("服务端操作日志将显示在这里...");
        serverLogArea.setEditable(false);
        JScrollPane serverScrollPane = new JScrollPane(serverLogArea);
        serverScrollPane.setBorder(new TitledBorder("服务端日志"));

        clientResultArea = new JTextArea("客户端测试结果将显示在这里...");
        clientResultArea.setEditable(false);
        JScrollPane clientScrollPane = new JScrollPane(clientResultArea);
        clientScrollPane.setBorder(new TitledBorder("客户端结果"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, serverScrollPane, clientScrollPane);
        splitPane.setResizeWeight(0.4); 

        add(splitPane, BorderLayout.CENTER);
    }
}
