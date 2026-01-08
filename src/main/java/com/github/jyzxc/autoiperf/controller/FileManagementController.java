package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.service.FileManagementService;
import com.github.jyzxc.autoiperf.ui.FileManagementPanel;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class FileManagementController {

    private static final Logger log = LoggerFactory.getLogger(FileManagementController.class);
    private final FileManagementPanel view;
    private final FileManagementService service;

    public FileManagementController(FileManagementPanel view, FileManagementService service) {
        this.view = view;
        this.service = service;
        addListeners();
    }

    private void addListeners() {
        view.getRefreshServerButton().addActionListener(e -> handleRefreshServerFiles());
        view.getRefreshClientButton().addActionListener(e -> handleRefreshClientFiles());
        view.getDeleteServerFileButton().addActionListener(e -> handleDeleteServerFile());
        view.getDeleteClientFileButton().addActionListener(e -> handleDeleteClientFile());
        
        // File content viewing removed as requested
    }

    private void handleRefreshServerFiles() {
        RemoteMachinePanel remotePanel = view.getServerRemotePanel();
        if (!remotePanel.isConnected()) {
            JOptionPane.showMessageDialog(view, "请先连接到服务端主机。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String host = remotePanel.getHostField().getText();
        view.getRefreshServerButton().setEnabled(false);
        view.getRefreshServerButton().setText("刷新中...");

        new SwingWorker<List<FileManagementService.JsonFileInfo>, Void>() {
            @Override
            protected List<FileManagementService.JsonFileInfo> doInBackground() throws Exception {
                return service.listJsonFiles(remotePanel.getSshService(), host);
            }

            @Override
            protected void done() {
                try {
                    List<FileManagementService.JsonFileInfo> files = get();
                    SwingUtilities.invokeLater(() -> {
                        updateServerFilesTable(files);
                        log.info("Refreshed server files: {} files found", files.size());
                    });
                } catch (Exception e) {
                    log.error("Failed to refresh server files", e);
                    JOptionPane.showMessageDialog(view, "刷新失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.getRefreshServerButton().setEnabled(true);
                    view.getRefreshServerButton().setText("刷新");
                }
            }
        }.execute();
    }

    private void handleRefreshClientFiles() {
        RemoteMachinePanel remotePanel = view.getClientRemotePanel();
        if (!remotePanel.isConnected()) {
            JOptionPane.showMessageDialog(view, "请先连接到客户端主机。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String host = remotePanel.getHostField().getText();
        view.getRefreshClientButton().setEnabled(false);
        view.getRefreshClientButton().setText("刷新中...");

        new SwingWorker<List<FileManagementService.JsonFileInfo>, Void>() {
            @Override
            protected List<FileManagementService.JsonFileInfo> doInBackground() throws Exception {
                return service.listJsonFiles(remotePanel.getSshService(), host);
            }

            @Override
            protected void done() {
                try {
                    List<FileManagementService.JsonFileInfo> files = get();
                    SwingUtilities.invokeLater(() -> {
                        updateClientFilesTable(files);
                        log.info("Refreshed client files: {} files found", files.size());
                    });
                } catch (Exception e) {
                    log.error("Failed to refresh client files", e);
                    JOptionPane.showMessageDialog(view, "刷新失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.getRefreshClientButton().setEnabled(true);
                    view.getRefreshClientButton().setText("刷新");
                }
            }
        }.execute();
    }

    private void updateServerFilesTable(List<FileManagementService.JsonFileInfo> files) {
        DefaultTableModel model = (DefaultTableModel) view.getServerFilesTable().getModel();
        model.setRowCount(0);

        int serverCount = 0;
        for (FileManagementService.JsonFileInfo file : files) {
            if ("server".equals(file.getType())) {
                model.addRow(new Object[]{
                        file.getFilename(),
                        file.getServerIp() != null ? file.getServerIp() : "N/A",
                        file.getPort() > 0 ? file.getPort() : "N/A",
                        file.getPid() != null ? file.getPid() : "N/A",
                        formatFileSize(file.getFileSize()),
                        file.getTimestamp() != null ? file.getTimestamp() : "N/A"
                });
                serverCount++;
            }
        }
        log.info("Updated server files table with {} server files (total files: {})", serverCount, files.size());
    }

    private void updateClientFilesTable(List<FileManagementService.JsonFileInfo> files) {
        DefaultTableModel model = (DefaultTableModel) view.getClientFilesTable().getModel();
        model.setRowCount(0);

        int clientCount = 0;
        for (FileManagementService.JsonFileInfo file : files) {
            if ("client".equals(file.getType())) {
                model.addRow(new Object[]{
                        file.getFilename(),
                        file.getServerIp() != null ? file.getServerIp() : "N/A",
                        file.getPort() > 0 ? file.getPort() : "N/A",
                        file.getClientIp() != null ? file.getClientIp() : "N/A",
                        file.getPid() != null ? file.getPid() : "N/A",
                        formatFileSize(file.getFileSize()),
                        file.getTimestamp() != null ? file.getTimestamp() : "N/A"
                });
                clientCount++;
            }
        }
        log.info("Updated client files table with {} client files (total files: {})", clientCount, files.size());
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }


    private void handleDeleteServerFile() {
        handleDeleteFile(true);
    }

    private void handleDeleteClientFile() {
        handleDeleteFile(false);
    }

    private void handleDeleteFile(boolean isServer) {
        JTable table = isServer ? view.getServerFilesTable() : view.getClientFilesTable();
        RemoteMachinePanel remotePanel = isServer ? view.getServerRemotePanel() : view.getClientRemotePanel();
        
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(view, "请先选择一个文件。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!remotePanel.isConnected()) {
            JOptionPane.showMessageDialog(view, "请先连接到主机。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String filename = (String) table.getModel().getValueAt(selectedRow, 0);
        String filePath = "/tmp/iperf3/" + filename;

        int confirm = JOptionPane.showConfirmDialog(view, 
                "确定要删除文件 " + filename + " 吗？", 
                "确认删除", 
                JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.deleteFile(remotePanel.getSshService(), filePath);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(view, "文件删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    // Refresh the table
                    if (isServer) {
                        handleRefreshServerFiles();
                    } else {
                        handleRefreshClientFiles();
                    }
                } catch (Exception e) {
                    log.error("Failed to delete file", e);
                    JOptionPane.showMessageDialog(view, "删除失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}

