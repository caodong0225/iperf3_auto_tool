package com.github.jyzxc.autoiperf.ui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

@Getter
public class FileManagementPanel extends JPanel {

    private final RemoteMachinePanel serverRemotePanel;
    private final RemoteMachinePanel clientRemotePanel;
    private final JTable serverFilesTable;
    private final JTable clientFilesTable;
    private final JButton refreshServerButton;
    private final JButton refreshClientButton;
    private final JButton deleteServerFileButton;
    private final JButton deleteClientFileButton;
    private final JTextArea fileContentArea; // Not used, kept for compatibility

    public FileManagementPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("文件管理"));

        // Top: Server and Client remote panels
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        
        // Server section
        JPanel serverPanel = new JPanel(new BorderLayout(5, 5));
        serverPanel.setBorder(new TitledBorder("服务端主机"));
        serverRemotePanel = new RemoteMachinePanel("服务端主机");
        serverPanel.add(serverRemotePanel, BorderLayout.NORTH);
        
        // Server files table
        String[] serverColumnNames = {"文件名", "服务器IP", "端口", "PID", "文件大小", "时间戳"};
        DefaultTableModel serverTableModel = new DefaultTableModel(serverColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serverFilesTable = new JTable(serverTableModel);
        serverFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        serverFilesTable.getColumnModel().getColumn(0).setPreferredWidth(200);  // 文件名
        serverFilesTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // 服务器IP
        serverFilesTable.getColumnModel().getColumn(2).setPreferredWidth(80);   // 端口
        serverFilesTable.getColumnModel().getColumn(3).setPreferredWidth(80);   // PID
        serverFilesTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // 文件大小
        serverFilesTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // 时间戳
        
        JScrollPane serverTableScrollPane = new JScrollPane(serverFilesTable);
        serverTableScrollPane.setBorder(new TitledBorder("服务端JSON文件"));
        serverTableScrollPane.setPreferredSize(new Dimension(600, 200));
        
        JPanel serverButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshServerButton = new JButton("刷新");
        deleteServerFileButton = new JButton("删除选中");
        serverButtonPanel.add(refreshServerButton);
        serverButtonPanel.add(deleteServerFileButton);
        
        serverPanel.add(serverTableScrollPane, BorderLayout.CENTER);
        serverPanel.add(serverButtonPanel, BorderLayout.SOUTH);
        
        // Client section
        JPanel clientPanel = new JPanel(new BorderLayout(5, 5));
        clientPanel.setBorder(new TitledBorder("客户端主机"));
        clientRemotePanel = new RemoteMachinePanel("客户端主机");
        clientPanel.add(clientRemotePanel, BorderLayout.NORTH);
        
        // Client files table
        String[] clientColumnNames = {"文件名", "服务器IP", "端口", "客户端IP", "PID", "文件大小", "时间戳"};
        DefaultTableModel clientTableModel = new DefaultTableModel(clientColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        clientFilesTable = new JTable(clientTableModel);
        clientFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        clientFilesTable.getColumnModel().getColumn(0).setPreferredWidth(200);  // 文件名
        clientFilesTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // 服务器IP
        clientFilesTable.getColumnModel().getColumn(2).setPreferredWidth(80);   // 端口
        clientFilesTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // 客户端IP
        clientFilesTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // PID
        clientFilesTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // 文件大小
        clientFilesTable.getColumnModel().getColumn(6).setPreferredWidth(150);  // 时间戳
        
        JScrollPane clientTableScrollPane = new JScrollPane(clientFilesTable);
        clientTableScrollPane.setBorder(new TitledBorder("客户端JSON文件"));
        clientTableScrollPane.setPreferredSize(new Dimension(600, 200));
        
        JPanel clientButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshClientButton = new JButton("刷新");
        deleteClientFileButton = new JButton("删除选中");
        clientButtonPanel.add(refreshClientButton);
        clientButtonPanel.add(deleteClientFileButton);
        
        clientPanel.add(clientTableScrollPane, BorderLayout.CENTER);
        clientPanel.add(clientButtonPanel, BorderLayout.SOUTH);
        
        topPanel.add(serverPanel);
        topPanel.add(clientPanel);
        
        // No file content viewer - removed as requested
        fileContentArea = null;
        
        add(topPanel, BorderLayout.CENTER);
    }
}

