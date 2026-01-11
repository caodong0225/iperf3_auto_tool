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
        // PID and Type columns removed as requested
        // Added: 发送速率, 接收速率, 发送CPU, 接收CPU
        String[] serverColumnNames = {"文件名", "服务器IP", "端口", "客户端IP", "文件大小", "时间戳", "发送速率(bps)", "接收速率(bps)", "发送CPU(%)", "接收CPU(%)"};
        DefaultTableModel serverTableModel = new DefaultTableModel(serverColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serverFilesTable = new JTable(serverTableModel);
        
        // Enable sorting for all columns
        serverFilesTable.setAutoCreateRowSorter(true);
        
        serverFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        serverFilesTable.setRowSelectionAllowed(true);
        serverFilesTable.setColumnSelectionAllowed(false);
        serverFilesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        serverFilesTable.getColumnModel().getColumn(0).setPreferredWidth(200);  // 文件名
        serverFilesTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // 服务器IP
        serverFilesTable.getColumnModel().getColumn(2).setPreferredWidth(80);   // 端口
        serverFilesTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // 客户端IP
        serverFilesTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // 文件大小
        serverFilesTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // 时间戳
        serverFilesTable.getColumnModel().getColumn(6).setPreferredWidth(120);  // 发送速率
        serverFilesTable.getColumnModel().getColumn(7).setPreferredWidth(120);  // 接收速率
        serverFilesTable.getColumnModel().getColumn(8).setPreferredWidth(100);  // 发送CPU
        serverFilesTable.getColumnModel().getColumn(9).setPreferredWidth(100);  // 接收CPU
        
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

