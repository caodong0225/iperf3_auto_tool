package com.github.jyzxc.autoiperf.ui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

@Getter
public class FileManagementPanel extends JPanel {

    private final RemoteMachinePanel serverRemotePanel;
    private final JTable serverFilesTable;
    private final JButton refreshServerButton;
    private final JButton deleteServerFileButton;

    public FileManagementPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("文件管理"));

        // Server section only (client section removed as requested)
        JPanel serverPanel = new JPanel(new BorderLayout(5, 5));
        serverPanel.setBorder(new TitledBorder("服务端主机"));
        serverRemotePanel = new RemoteMachinePanel("服务端主机");
        serverPanel.add(serverRemotePanel, BorderLayout.NORTH);

        // Server files table (shows all json files under /tmp/iperf3 on the server host)
        // Include Type + Client IP for compatibility when server host also contains client-result files.
        String[] serverColumnNames = {"文件名", "类型", "服务器IP", "端口", "客户端IP", "PID", "文件大小", "时间戳"};
        DefaultTableModel serverTableModel = new DefaultTableModel(serverColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serverFilesTable = new JTable(serverTableModel);
        serverFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        serverFilesTable.getColumnModel().getColumn(0).setPreferredWidth(200);  // 文件名
        serverFilesTable.getColumnModel().getColumn(1).setPreferredWidth(70);   // 类型
        serverFilesTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // 服务器IP
        serverFilesTable.getColumnModel().getColumn(3).setPreferredWidth(80);   // 端口
        serverFilesTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // 客户端IP
        serverFilesTable.getColumnModel().getColumn(5).setPreferredWidth(80);   // PID
        serverFilesTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // 文件大小
        serverFilesTable.getColumnModel().getColumn(7).setPreferredWidth(150);  // 时间戳
        
        JScrollPane serverTableScrollPane = new JScrollPane(serverFilesTable);
        serverTableScrollPane.setBorder(new TitledBorder("服务端 /tmp/iperf3 JSON 文件"));
        serverTableScrollPane.setPreferredSize(new Dimension(600, 200));
        
        JPanel serverButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshServerButton = new JButton("刷新");
        deleteServerFileButton = new JButton("删除选中");
        serverButtonPanel.add(refreshServerButton);
        serverButtonPanel.add(deleteServerFileButton);
        
        serverPanel.add(serverTableScrollPane, BorderLayout.CENTER);
        serverPanel.add(serverButtonPanel, BorderLayout.SOUTH);

        add(serverPanel, BorderLayout.CENTER);
    }
}

