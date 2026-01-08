package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.model.IperfServerInstance;
import com.github.jyzxc.autoiperf.service.ServerManagerService;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import com.github.jyzxc.autoiperf.ui.ResultsPanel;
import com.github.jyzxc.autoiperf.ui.ServerControlPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ServerControlController {

    private static final Logger log = LoggerFactory.getLogger(ServerControlController.class);

    private final ServerControlPanel view;
    private final ServerManagerService service;
    private final ResultsPanel resultsPanel;
    private Timer refreshTimer;

    public ServerControlController(ServerControlPanel view, ServerManagerService service, ResultsPanel resultsPanel) {
        this.view = view;
        this.service = service;
        this.resultsPanel = resultsPanel;
        addListeners();
        startAutoRefresh();
    }
    
    /**
     * Start automatic refresh timer that updates the table every 10 seconds.
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer(10000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();
                if (remoteMachinePanel != null && remoteMachinePanel.isConnected()) {
                    log.debug("Auto-refreshing server instances table...");
                    handleDiscoverInstances(true);
                }
            }
        });
        refreshTimer.setRepeats(true);
        refreshTimer.start();
        log.info("Started auto-refresh timer for server instances (every 10 seconds)");
    }
    
    /**
     * Stop the auto-refresh timer.
     */
    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            log.info("Stopped auto-refresh timer for server instances");
        }
    }
    
    /**
     * Append log message to the server log area in the results panel.
     * @param message The log message to append
     */
    private void appendServerLog(String message) {
        if (resultsPanel != null && resultsPanel.getServerLogArea() != null) {
            SwingUtilities.invokeLater(() -> {
                JTextArea logArea = resultsPanel.getServerLogArea();
                String timestamp = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                logArea.append(String.format("[%s] %s%n", timestamp, message));
                // Auto-scroll to bottom
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    private void addListeners() {
        view.getStartServerButton().addActionListener(e -> handleStartServer());
        view.getRemoteMachinePanel().addConnectionStateListener(this::handleConnectionStateChange);
        view.getStopServerButton().addActionListener(e -> handleStopOrKillServer(false));
        view.getKillServerButton().addActionListener(e -> handleStopOrKillServer(true));
    }

    private void handleConnectionStateChange(boolean isConnected) {
        if (isConnected) {
            String host = view.getRemoteMachinePanel().getHostField().getText();
            log.info("Connection established, discovering running instances...");
            appendServerLog(String.format("已连接到服务端主机: %s", host));
            appendServerLog("正在发现运行中的 iperf3 实例...");
            handleDiscoverInstances();
            // Timer is already started in constructor, it will check connection status
        } else {
            log.info("Connection lost, clearing instance table.");
            appendServerLog("连接已断开，清空实例列表");
            clearInstanceTable();
            // Timer will continue but won't do anything since connection is false
        }
    }

    private void handleDiscoverInstances() {
        handleDiscoverInstances(false);
    }
    
    private void handleDiscoverInstances(boolean isAutoRefresh) {
        RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();
        String host = remoteMachinePanel.getHostField().getText();

        new SwingWorker<List<IperfServerInstance>, Void>() {
            @Override
            protected List<IperfServerInstance> doInBackground() throws Exception {
                return service.discoverRunningInstances(remoteMachinePanel.getSshService(), host);
            }

            @Override
            protected void done() {
                try {
                    List<IperfServerInstance> instances = get();
                    SwingUtilities.invokeLater(() -> {
                        updateInstanceTable(instances);
                        log.info("Successfully discovered {} running instances.", instances.size());
                        // Only log to console, not to GUI for auto-refresh
                        if (!isAutoRefresh) {
                            appendServerLog(String.format("发现 %d 个运行中的 iperf3 服务实例", instances.size()));
                            for (IperfServerInstance instance : instances) {
                                appendServerLog(String.format("  - PID: %d, 端口: %d, 绑定IP: %s, 状态: %s", 
                                        instance.getPid(), instance.getListeningPort(), 
                                        instance.getBoundIp(), instance.getStatus()));
                            }
                        } else {
                            log.debug("Auto-refresh: Found {} running server instances", instances.size());
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to discover running iperf3 instances.", e);
                    // Only show error dialog for manual refresh, not auto-refresh
                    if (!isAutoRefresh) {
                        appendServerLog(String.format("发现服务实例失败: %s", e.getMessage()));
                        JOptionPane.showMessageDialog(view, "发现服务实例失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    } else {
                        log.debug("Auto-refresh failed: {}", e.getMessage());
                    }
                }
            }
        }.execute();
    }

    private void handleStartServer() {
        log.info("'Start New Server' button clicked.");
        RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();

        if (!remoteMachinePanel.isConnected()) {
            JOptionPane.showMessageDialog(view, "请先连接到服务端主机。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectedIp = remoteMachinePanel.getSelectedNicIp();
        if (selectedIp == null || selectedIp.startsWith("待") || selectedIp.startsWith("未")) {
            JOptionPane.showMessageDialog(view, "请为服务端主机选择一个用于绑定的网卡IP。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port = (Integer) view.getPortSpinner().getValue();
        String host = remoteMachinePanel.getHostField().getText();

        appendServerLog(String.format("开始启动 iperf3 服务端: 主机=%s, 绑定IP=%s, 端口=%d", host, selectedIp, port));
        view.getStartServerButton().setEnabled(false);
        view.getStartServerButton().setText("正在启动...");

        new SwingWorker<IperfServerInstance, Void>() {
            @Override
            protected IperfServerInstance doInBackground() throws Exception {
                return service.startServer(remoteMachinePanel.getSshService(), host, port, selectedIp);
            }

            @Override
            protected void done() {
                try {
                    IperfServerInstance instance = get();
                    log.info("Server started successfully, adding to table: PID={}, Port={}, BindIP={}", 
                            instance.getPid(), instance.getListeningPort(), instance.getBoundIp());
                    appendServerLog(String.format("✓ iperf3 服务启动成功: PID=%d, 端口=%d, 绑定IP=%s", 
                            instance.getPid(), instance.getListeningPort(), instance.getBoundIp()));
                    SwingUtilities.invokeLater(() -> {
                        addInstanceToTable(instance);
                        log.info("Instance added to table. Current row count: {}", 
                                ((DefaultTableModel) view.getServerInstancesTable().getModel()).getRowCount());
                    });
                    JOptionPane.showMessageDialog(view, "iPerf3 服务已成功启动！\nPID: " + instance.getPid(), "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to start iperf3 server.", e);
                    
                    // Check if it's a port in use exception
                    Throwable cause = e;
                    if (e instanceof java.util.concurrent.ExecutionException) {
                        cause = e.getCause();
                    }
                    
                    if (cause instanceof com.github.jyzxc.autoiperf.model.PortInUseByIperfException) {
                        com.github.jyzxc.autoiperf.model.PortInUseByIperfException portException = 
                                (com.github.jyzxc.autoiperf.model.PortInUseByIperfException) cause;
                        
                        appendServerLog(String.format("✗ 端口被占用: %s", portException.getMessage()));
                        
                        // Show dialog asking user if they want to kill the process
                        int option = JOptionPane.showConfirmDialog(
                                view,
                                portException.getMessage() + "\n\n是否需要强制终止该进程并重试启动？",
                                "端口被占用",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );
                        
                        if (option == JOptionPane.YES_OPTION) {
                            // User wants to kill the process and retry
                            appendServerLog(String.format("用户选择终止占用进程 (PID: %s, 进程: %s)", 
                                    portException.getPid(), portException.getProcessName()));
                            handlePortConflictAndRetry(portException.getPid(), portException.getProcessName());
                            return; // Don't reset button state, retry will handle it
                        } else {
                            // User cancelled
                            appendServerLog("用户取消了操作");
                            JOptionPane.showMessageDialog(view, "操作已取消。", "提示", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        // Other errors
                        appendServerLog(String.format("✗ 启动服务失败: %s", e.getMessage()));
                        JOptionPane.showMessageDialog(view, "启动服务失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        view.getStartServerButton().setEnabled(true);
                        view.getStartServerButton().setText("开启新服务");
                    });
                }
            }
        }.execute();
    }

    private void handlePortConflictAndRetry(String blockingPid, String processName) {
        log.info("User chose to kill blocking process: PID={}, Process={}", blockingPid, processName);
        appendServerLog(String.format("正在终止占用进程: PID=%s, 进程名=%s", blockingPid, processName));
        RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();
        String host = remoteMachinePanel.getHostField().getText();
        int port = (Integer) view.getPortSpinner().getValue();
        String selectedIp = remoteMachinePanel.getSelectedNicIp();
        
        view.getStartServerButton().setText("正在终止占用进程...");
        
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Kill the blocking process
                try {
                    String killCommand = "kill -9 " + blockingPid;
                    remoteMachinePanel.getSshService().executeCommand(killCommand, 3000);
                    log.info("Successfully killed blocking process PID: {}", blockingPid);
                    // Wait a bit for the port to be released
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.error("Failed to kill blocking process: {}", e.getMessage());
                    throw new Exception("无法终止占用进程: " + e.getMessage());
                }
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    log.info("Blocking process killed, retrying server start...");
                    appendServerLog(String.format("✓ 占用进程已终止 (PID: %s)", blockingPid));
                    appendServerLog("正在重试启动 iperf3 服务...");
                    // Retry starting the server
                    retryStartServer(host, port, selectedIp);
                } catch (Exception e) {
                    log.error("Failed to kill blocking process", e);
                    appendServerLog(String.format("✗ 无法终止占用进程: %s", e.getMessage()));
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(view, 
                                "无法终止占用进程: \n" + e.getMessage(), 
                                "错误", 
                                JOptionPane.ERROR_MESSAGE);
                        view.getStartServerButton().setEnabled(true);
                        view.getStartServerButton().setText("开启新服务");
                    });
                }
            }
        }.execute();
    }
    
    private void retryStartServer(String host, int port, String selectedIp) {
        RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();
        view.getStartServerButton().setText("正在启动...");
        
        new SwingWorker<IperfServerInstance, Void>() {
            @Override
            protected IperfServerInstance doInBackground() throws Exception {
                return service.startServer(remoteMachinePanel.getSshService(), host, port, selectedIp);
            }
            
            @Override
            protected void done() {
                try {
                    IperfServerInstance instance = get();
                    log.info("Server started successfully after retry, adding to table: PID={}, Port={}, BindIP={}", 
                            instance.getPid(), instance.getListeningPort(), instance.getBoundIp());
                    appendServerLog(String.format("✓ 重试启动成功: PID=%d, 端口=%d, 绑定IP=%s", 
                            instance.getPid(), instance.getListeningPort(), instance.getBoundIp()));
                    SwingUtilities.invokeLater(() -> {
                        addInstanceToTable(instance);
                        log.info("Instance added to table. Current row count: {}", 
                                ((DefaultTableModel) view.getServerInstancesTable().getModel()).getRowCount());
                    });
                    JOptionPane.showMessageDialog(view, "iPerf3 服务已成功启动！\nPID: " + instance.getPid(), "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to start iperf3 server after retry.", e);
                    Throwable cause = e;
                    if (e instanceof java.util.concurrent.ExecutionException) {
                        cause = e.getCause();
                    }
                    
                    if (cause instanceof com.github.jyzxc.autoiperf.model.PortInUseByIperfException) {
                        // Port still in use after kill attempt
                        appendServerLog(String.format("✗ 重试失败: 端口仍然被占用 - %s", cause.getMessage()));
                        JOptionPane.showMessageDialog(view, 
                                "端口仍然被占用，启动失败。\n" + cause.getMessage(), 
                                "错误", 
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        appendServerLog(String.format("✗ 重试启动失败: %s", e.getMessage()));
                        JOptionPane.showMessageDialog(view, "启动服务失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        view.getStartServerButton().setEnabled(true);
                        view.getStartServerButton().setText("开启新服务");
                    });
                }
            }
        }.execute();
    }

    private void handleStopOrKillServer(boolean force) {
        JTable table = view.getServerInstancesTable();
        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(view, "请先在表格中选择一个要操作的服务实例。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // PID is in the first column (index 0)
        int pid = (Integer) table.getModel().getValueAt(selectedRow, 0);
        String action = force ? "强制终止" : "停止";
        log.info("User requested to {} server with PID {}", action.toLowerCase(), pid);
        appendServerLog(String.format("用户请求%s服务: PID=%d", action, pid));

        view.getStopServerButton().setEnabled(false);
        view.getKillServerButton().setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();
                if (force) {
                    service.killServer(remoteMachinePanel.getSshService(), pid);
                } else {
                    service.stopServer(remoteMachinePanel.getSshService(), pid);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions from the background task
                    log.info("Successfully {} process with PID {}", action.toLowerCase(), pid);
                    appendServerLog(String.format("✓ 成功%s进程: PID=%d", action, pid));
                    ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                    JOptionPane.showMessageDialog(view, "PID: " + pid + " 已被成功" + action + "。", "操作成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to {} process with PID {}", action.toLowerCase(), pid, e);
                    appendServerLog(String.format("✗ %s进程失败: PID=%d, 错误=%s", action, pid, e.getMessage()));
                    JOptionPane.showMessageDialog(view, action + " PID: " + pid + " 失败: \n" + e.getMessage(), "操作失败", JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.getStopServerButton().setEnabled(true);
                    view.getKillServerButton().setEnabled(true);
                }
            }
        }.execute();
    }

    private void addInstanceToTable(IperfServerInstance instance) {
        DefaultTableModel model = (DefaultTableModel) view.getServerInstancesTable().getModel();
        Object[] rowData = new Object[]{
                instance.getPid(),
                instance.getStatus() != null ? instance.getStatus().toString() : "UNKNOWN",
                instance.getBoundIp() != null ? instance.getBoundIp() : "N/A",
                instance.getListeningPort(),
                instance.getCommandLine() != null ? instance.getCommandLine() : "N/A"
        };
        model.addRow(rowData);
        log.info("Added row to table: PID={}, Status={}, Port={}, BindIP={}, row count now={}", 
                instance.getPid(), 
                instance.getStatus(),
                instance.getListeningPort(),
                instance.getBoundIp(),
                model.getRowCount());
        
        // Force table refresh
        view.getServerInstancesTable().revalidate();
        view.getServerInstancesTable().repaint();
    }

    private void updateInstanceTable(List<IperfServerInstance> instances) {
        log.info("Attempting to update table with {} instances.", instances.size());
        DefaultTableModel model = (DefaultTableModel) view.getServerInstancesTable().getModel();
        
        log.info("Table model row count before update: {}", model.getRowCount());
        model.setRowCount(0); // Clear table
        log.info("Table model row count after clearing: {}", model.getRowCount());

        for (IperfServerInstance instance : instances) {
            log.info("Adding instance to table: PID={}, Status={}, Port={}, BindIP={}", 
                    instance.getPid(), instance.getStatus(), instance.getListeningPort(), instance.getBoundIp());
            addInstanceToTable(instance);
        }
        
        log.info("Table model row count after adding all instances: {}", model.getRowCount());
        
        // Force UI refresh - already on EDT, but ensure visibility
        JTable table = view.getServerInstancesTable();
        table.revalidate();
        table.repaint();
        
        // Ensure table columns are visible by resetting column widths if needed
        if (model.getRowCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);  // PID
            table.getColumnModel().getColumn(1).setPreferredWidth(80);  // 状态
            table.getColumnModel().getColumn(2).setPreferredWidth(120); // 监听IP
            table.getColumnModel().getColumn(3).setPreferredWidth(80);  // 端口
            table.getColumnModel().getColumn(4).setPreferredWidth(300); // 完整命令
            table.sizeColumnsToFit(-1);
        }
        
        // Log table visibility and size for debugging
        log.info("Table is visible: {}, showing: {}, size: {}x{}, row height: {}", 
                table.isVisible(), 
                table.isShowing(),
                table.getWidth(),
                table.getHeight(),
                table.getRowHeight());
        
        // Log scroll pane info
        Container parent = table.getParent();
        if (parent instanceof JViewport) {
            JViewport viewport = (JViewport) parent;
            log.info("Viewport size: {}x{}", viewport.getWidth(), viewport.getHeight());
            Container scrollPane = viewport.getParent();
            if (scrollPane instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) scrollPane;
                log.info("ScrollPane size: {}x{}, visible: {}, preferred: {}x{}, min: {}x{}", 
                        sp.getWidth(),
                        sp.getHeight(),
                        sp.isVisible(),
                        sp.getPreferredSize().width,
                        sp.getPreferredSize().height,
                        sp.getMinimumSize().width,
                        sp.getMinimumSize().height);
            }
        }
        
        // Force layout update if table height is 0
        if (table.getHeight() == 0) {
            log.warn("Table height is 0, forcing layout update...");
            SwingUtilities.invokeLater(() -> {
                Container root = table.getTopLevelAncestor();
                if (root != null) {
                    root.validate();
                    root.repaint();
                }
                // Also validate the scroll pane
                if (parent instanceof JViewport) {
                    Container scrollPane = parent.getParent();
                    if (scrollPane instanceof JScrollPane) {
                        ((JScrollPane) scrollPane).validate();
                        ((JScrollPane) scrollPane).repaint();
                    }
                }
            });
        }
    }

    private void clearInstanceTable() {
        DefaultTableModel model = (DefaultTableModel) view.getServerInstancesTable().getModel();
        model.setRowCount(0);
    }
}
