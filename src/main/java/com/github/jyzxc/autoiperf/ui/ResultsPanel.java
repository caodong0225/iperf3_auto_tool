package com.github.jyzxc.autoiperf.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel on the right to display test results.
 */
public class ResultsPanel extends JPanel {
    public ResultsPanel() {
        setBorder(new TitledBorder("测试结果"));
        setLayout(new BorderLayout());

        JTextArea resultsArea = new JTextArea("测试结果将显示在这里...");
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultsArea.setMargin(new Insets(5,5,5,5));

        JScrollPane scrollPane = new JScrollPane(resultsArea);
        add(scrollPane, BorderLayout.CENTER);
    }
}
