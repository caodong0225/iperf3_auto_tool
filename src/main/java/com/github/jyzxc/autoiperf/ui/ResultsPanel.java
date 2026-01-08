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
        
        serverLogArea = new JTextArea();
        serverLogArea.setEditable(false);
        serverLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        serverLogArea.setLineWrap(true);
        serverLogArea.setWrapStyleWord(true);
        serverLogArea.setText("服务端操作日志将显示在这里...\n");
        JScrollPane serverScrollPane = new JScrollPane(serverLogArea);
        serverScrollPane.setBorder(new TitledBorder("服务端日志"));
        serverScrollPane.setPreferredSize(new Dimension(400, 200));

        clientResultArea = new JTextArea();
        clientResultArea.setEditable(false);
        clientResultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        clientResultArea.setLineWrap(true);
        clientResultArea.setWrapStyleWord(true);
        clientResultArea.setText("客户端测试日志将显示在这里...\n");
        JScrollPane clientScrollPane = new JScrollPane(clientResultArea);
        clientScrollPane.setBorder(new TitledBorder("客户端日志"));
        clientScrollPane.setPreferredSize(new Dimension(400, 200));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, serverScrollPane, clientScrollPane);
        splitPane.setResizeWeight(0.4); 

        add(splitPane, BorderLayout.CENTER);
    }
}
