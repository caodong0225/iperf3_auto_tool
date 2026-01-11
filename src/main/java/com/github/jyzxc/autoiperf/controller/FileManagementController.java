package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.service.FileManagementService;
import com.github.jyzxc.autoiperf.ui.FileManagementPanel;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
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
        view.getDeleteServerFileButton().addActionListener(e -> handleDeleteServerFile());
        // Listen for connection state changes - auto refresh files when connected
        view.getServerRemotePanel().addConnectionStateListener(isConnected -> {
            if (isConnected) {
                log.info("File management connection established, auto-refreshing file list...");
                SwingUtilities.invokeLater(() -> handleRefreshServerFiles());
            }
        });
        installTableContextMenu();
        
        // File content viewing removed as requested
    }

    private void installTableContextMenu() {
        JTable table = view.getServerFilesTable();
        JPopupMenu menu = new JPopupMenu();

        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> handleDeleteServerFile());

        JMenuItem downloadItem = new JMenuItem("下载...");
        downloadItem.addActionListener(e -> handleDownloadSelectedFile());

        menu.add(downloadItem);
        menu.add(deleteItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0 && viewRow < table.getRowCount()) {
                    // If right-click is on an unselected row, switch selection to that row.
                    // If it's on a selected row, keep existing multi-selection.
                    if (!table.isRowSelected(viewRow)) {
                        table.setRowSelectionInterval(viewRow, viewRow);
                    }
                } else {
                    table.clearSelection();
                }

                boolean hasSelection = table.getSelectedRowCount() > 0;
                downloadItem.setEnabled(hasSelection);
                deleteItem.setEnabled(hasSelection);

                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
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

    private void updateServerFilesTable(List<FileManagementService.JsonFileInfo> files) {
        DefaultTableModel model = (DefaultTableModel) view.getServerFilesTable().getModel();
        model.setRowCount(0);

        // Columns: 文件名, 服务器IP, 端口, 客户端IP, 文件大小, 时间戳, 发送速率, 接收速率, 发送CPU, 接收CPU
        for (FileManagementService.JsonFileInfo file : files) {
            model.addRow(new Object[]{
                    file.getFilename(),
                    file.getServerIp() != null ? file.getServerIp() : "N/A",
                    file.getPort() > 0 ? file.getPort() : "N/A",
                    file.getClientIp() != null ? file.getClientIp() : "N/A",
                    formatFileSize(file.getFileSize()),
                    file.getTimestamp() != null ? file.getTimestamp() : "N/A",
                    formatRate(file.getSendRate()),
                    formatRate(file.getReceiveRate()),
                    formatCpu(file.getSendCpu()),
                    formatCpu(file.getReceiveCpu())
            });
        }
        log.info("Updated server files table with {} files", files.size());
    }

    private String formatRate(Double rate) {
        if (rate == null) {
            return "N/A";
        }
        if (rate >= 1_000_000_000) {
            return String.format("%.2f Gbps", rate / 1_000_000_000.0);
        } else if (rate >= 1_000_000) {
            return String.format("%.2f Mbps", rate / 1_000_000.0);
        } else if (rate >= 1_000) {
            return String.format("%.2f Kbps", rate / 1_000.0);
        } else {
            return String.format("%.2f bps", rate);
        }
    }

    private String formatCpu(Double cpu) {
        if (cpu == null) {
            return "N/A";
        }
        return String.format("%.2f%%", cpu);
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

    private void handleDeleteFile(boolean isServer) {
        JTable table = view.getServerFilesTable();
        RemoteMachinePanel remotePanel = view.getServerRemotePanel();
        
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            JOptionPane.showMessageDialog(view, "请先选择一个文件。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!remotePanel.isConnected()) {
            JOptionPane.showMessageDialog(view, "请先连接到主机。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> filePaths = new ArrayList<>();
        for (int viewRow : selectedRows) {
            // Convert view row index to model row index when table has sorting enabled
            int modelRow = table.convertRowIndexToModel(viewRow);
            String filename = (String) table.getModel().getValueAt(modelRow, 0);
            if (filename == null || filename.isBlank()) {
                continue;
            }
            filePaths.add("/tmp/iperf3/" + filename);
        }
        if (filePaths.isEmpty()) {
            JOptionPane.showMessageDialog(view, "请选择有效的文件。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(view, 
                "确定要删除选中的 " + filePaths.size() + " 个文件吗？", 
                "确认删除", 
                JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (String path : filePaths) {
                    service.deleteFile(remotePanel.getSshService(), path);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(view, "删除成功：共 " + filePaths.size() + " 个文件。", "成功", JOptionPane.INFORMATION_MESSAGE);
                    // Refresh the table
                    handleRefreshServerFiles();
                } catch (Exception e) {
                    log.error("Failed to delete file", e);
                    JOptionPane.showMessageDialog(view, "删除失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleDownloadSelectedFile() {
        JTable table = view.getServerFilesTable();
        RemoteMachinePanel remotePanel = view.getServerRemotePanel();

        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            JOptionPane.showMessageDialog(view, "请先选择一个文件。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!remotePanel.isConnected()) {
            JOptionPane.showMessageDialog(view, "请先连接到服务端主机。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> filenames = new ArrayList<>();
        List<String> remotePaths = new ArrayList<>();
        for (int viewRow : selectedRows) {
            // Convert view row index to model row index when table has sorting enabled
            int modelRow = table.convertRowIndexToModel(viewRow);
            String filename = (String) table.getModel().getValueAt(modelRow, 0);
            if (filename == null || filename.isBlank()) {
                continue;
            }
            filenames.add(filename);
            remotePaths.add("/tmp/iperf3/" + filename);
        }
        if (remotePaths.isEmpty()) {
            JOptionPane.showMessageDialog(view, "请选择有效的文件。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File[] localTargets = new File[remotePaths.size()];
        if (remotePaths.size() == 1) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("保存文件到本地");
            chooser.setSelectedFile(new File(filenames.get(0)));
            int result = chooser.showSaveDialog(view);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            localTargets[0] = chooser.getSelectedFile();
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择本地保存目录");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            int result = chooser.showOpenDialog(view);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File dir = chooser.getSelectedFile();
            for (int i = 0; i < filenames.size(); i++) {
                localTargets[i] = new File(dir, filenames.get(i));
            }
        }
        view.getRefreshServerButton().setEnabled(false);
        view.getDeleteServerFileButton().setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i < remotePaths.size(); i++) {
                    service.downloadFile(remotePanel.getSshService(), remotePaths.get(i), localTargets[i].getAbsolutePath());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(view,
                            "下载成功：共 " + remotePaths.size() + " 个文件。",
                            "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to download file", e);
                    JOptionPane.showMessageDialog(view, "下载失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.getRefreshServerButton().setEnabled(true);
                    view.getDeleteServerFileButton().setEnabled(true);
                }
            }
        }.execute();
    }
}

