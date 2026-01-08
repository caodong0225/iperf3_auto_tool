package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.model.ClientTestConfig;
import com.github.jyzxc.autoiperf.model.ClientTestInstance;
import com.github.jyzxc.autoiperf.model.ClientTestResult;
import com.github.jyzxc.autoiperf.model.SshProfile;
import com.github.jyzxc.autoiperf.service.ClientManagerService;
import com.github.jyzxc.autoiperf.service.ClientTestService;
import com.github.jyzxc.autoiperf.ui.ClientControlPanel;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import com.github.jyzxc.autoiperf.ui.ResultsPanel;
import com.github.jyzxc.autoiperf.ui.ServerControlPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientControlController {

    private static final Logger log = LoggerFactory.getLogger(ClientControlController.class);

    private final ClientControlPanel view;
    private final ClientTestService clientTestService;
    private final ClientManagerService clientManagerService;
    private final ResultsPanel resultsPanel;
    private final ServerControlPanel serverControlPanel; // Reference to server panel to get selected instance

    public ClientControlController(ClientControlPanel view, 
                                   ClientTestService clientTestService,
                                   ClientManagerService clientManagerService,
                                   ResultsPanel resultsPanel,
                                   ServerControlPanel serverControlPanel) {
        this.view = view;
        this.clientTestService = clientTestService;
        this.clientManagerService = clientManagerService;
        this.resultsPanel = resultsPanel;
        this.serverControlPanel = serverControlPanel;
        addListeners();
    }

    /**
     * Append log message to the client result area in the results panel.
     */
    private void appendClientLog(String message) {
        if (resultsPanel != null && resultsPanel.getClientResultArea() != null) {
            SwingUtilities.invokeLater(() -> {
                JTextArea logArea = resultsPanel.getClientResultArea();
                String timestamp = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                logArea.append(String.format("[%s] %s%n", timestamp, message));
                // Auto-scroll to bottom
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    private void addListeners() {
        view.getStartTestButton().addActionListener(e -> handleStartTest());
        view.getRemoteMachinePanel().addConnectionStateListener(this::handleConnectionStateChange);
        view.getStopTestButton().addActionListener(e -> handleStopOrKillTest(false));
        
        // Add listener to server table selection to auto-fill target IP and port
        serverControlPanel.getServerInstancesTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleServerInstanceSelection();
            }
        });
    }

    private void handleServerInstanceSelection() {
        JTable serverTable = serverControlPanel.getServerInstancesTable();
        int selectedRow = serverTable.getSelectedRow();
        
        if (selectedRow >= 0) {
            String boundIp = (String) serverTable.getModel().getValueAt(selectedRow, 2);
            Integer port = (Integer) serverTable.getModel().getValueAt(selectedRow, 3);
            
            if (boundIp != null && !boundIp.equals("N/A")) {
                view.getTargetIpField().setText(boundIp);
            }
            if (port != null) {
                view.getTargetPortSpinner().setValue(port);
            }
            
            appendClientLog(String.format("已选择服务端实例: IP=%s, 端口=%d", boundIp, port));
        }
    }

    private void handleConnectionStateChange(boolean isConnected) {
        if (isConnected) {
            String host = view.getRemoteMachinePanel().getHostField().getText();
            log.info("Client connection established, discovering running client tests...");
            appendClientLog(String.format("已连接到客户端主机: %s", host));
            appendClientLog("正在发现运行中的 iperf3 客户端测试...");
            handleDiscoverClientTests();
        } else {
            log.info("Client connection lost, clearing test table.");
            appendClientLog("连接已断开，清空测试列表");
            clearTestTable();
        }
    }

    private void handleDiscoverClientTests() {
        RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();
        String host = remoteMachinePanel.getHostField().getText();

        new SwingWorker<List<ClientTestInstance>, Void>() {
            @Override
            protected List<ClientTestInstance> doInBackground() throws Exception {
                return clientManagerService.discoverRunningClientTests(remoteMachinePanel.getSshService(), host);
            }

            @Override
            protected void done() {
                try {
                    List<ClientTestInstance> instances = get();
                    SwingUtilities.invokeLater(() -> {
                        updateTestTable(instances);
                        log.info("Successfully discovered {} running client tests.", instances.size());
                        appendClientLog(String.format("发现 %d 个运行中的 iperf3 客户端测试", instances.size()));
                        for (ClientTestInstance instance : instances) {
                            appendClientLog(String.format("  - PID: %d, 目标: %s:%d, 状态: %s", 
                                    instance.getPid(), instance.getTargetHost(), 
                                    instance.getTargetPort(), instance.getStatus()));
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to discover running client tests.", e);
                    appendClientLog(String.format("发现客户端测试失败: %s", e.getMessage()));
                    JOptionPane.showMessageDialog(view, "发现客户端测试失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleStartTest() {
        log.info("'Start New Test' button clicked.");
        RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();

        if (!remoteMachinePanel.isConnected()) {
            JOptionPane.showMessageDialog(view, "请先连接到客户端主机。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String targetIp = view.getTargetIpField().getText().trim();
        if (targetIp.isEmpty()) {
            JOptionPane.showMessageDialog(view, "请输入目标IP地址。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int targetPort = (Integer) view.getTargetPortSpinner().getValue();
        int duration = (Integer) view.getDurationSpinner().getValue();
        String protocol = (String) view.getProtocolComboBox().getSelectedItem();
        String host = remoteMachinePanel.getHostField().getText();
        String selectedIp = remoteMachinePanel.getSelectedNicIp();

        appendClientLog(String.format("开始客户端测试: 目标=%s:%d, 时长=%d秒, 协议=%s", targetIp, targetPort, duration, protocol));
        view.getStartTestButton().setEnabled(false);
        view.getStartTestButton().setText("正在测试...");

        // Build SshProfile for the client machine (not actually used, but required by config)
        // The actual SSH connection is already established via remoteMachinePanel
        SshProfile clientProfile = new SshProfile();
        clientProfile.setHost(host);
        clientProfile.setUsername(""); // Not needed for test execution
        clientProfile.setPassword(""); // Not needed for test execution

        // Build ClientTestConfig
        ClientTestConfig config = ClientTestConfig.builder()
                .sourceMachineProfile(clientProfile)
                .sourceBindAddress(selectedIp)
                .targetHost(targetIp)
                .targetPort(targetPort)
                .duration(duration)
                .protocol(protocol)
                .build();

        new SwingWorker<ClientTestResult, Void>() {
            @Override
            protected ClientTestResult doInBackground() throws Exception {
                return clientTestService.executeTest(remoteMachinePanel.getSshService(), config);
            }

            @Override
            protected void done() {
                try {
                    ClientTestResult result = get();
                    log.info("Client test completed successfully: testId={}", result.getTestId());
                    
                    if (result.isSuccess()) {
                        appendClientLog(String.format("✓ 测试完成: 测试ID=%s", result.getTestId()));
                        if (result.getClientJsonResult() != null && result.getClientJsonResult().getEnd() != null) {
                            if (result.getClientJsonResult().getEnd().getSumReceived() != null) {
                                double bitsPerSecond = result.getClientJsonResult().getEnd().getSumReceived().getBitsPerSecond();
                                appendClientLog(String.format("  平均速率: %.2f Mbps", bitsPerSecond / 1_000_000));
                            }
                        }
                    } else {
                        appendClientLog(String.format("✗ 测试失败: %s", result.getErrorMessage()));
                    }
                    
                    // Add to table
                    SwingUtilities.invokeLater(() -> {
                        addTestToTable(result);
                    });
                    
                    JOptionPane.showMessageDialog(view, 
                            result.isSuccess() ? "测试完成！" : "测试失败: " + result.getErrorMessage(), 
                            result.isSuccess() ? "成功" : "错误", 
                            result.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to execute client test.", e);
                    appendClientLog(String.format("✗ 测试执行失败: %s", e.getMessage()));
                    JOptionPane.showMessageDialog(view, "测试失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        view.getStartTestButton().setEnabled(true);
                        view.getStartTestButton().setText("开始新测试");
                    });
                }
            }
        }.execute();
    }

    private void handleStopOrKillTest(boolean force) {
        JTable table = view.getClientProcessesTable();
        int selectedRow = table.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(view, "请先在表格中选择一个要操作的测试。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int pid = (Integer) table.getModel().getValueAt(selectedRow, 0);
        String action = force ? "强制终止" : "终止";
        log.info("User requested to {} test with PID {}", action.toLowerCase(), pid);
        appendClientLog(String.format("用户请求%s测试: PID=%d", action, pid));

        view.getStopTestButton().setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                RemoteMachinePanel remoteMachinePanel = view.getRemoteMachinePanel();
                if (force) {
                    clientManagerService.killClientTest(remoteMachinePanel.getSshService(), pid);
                } else {
                    clientManagerService.stopClientTest(remoteMachinePanel.getSshService(), pid);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    log.info("Successfully {} test process with PID {}", action.toLowerCase(), pid);
                    appendClientLog(String.format("✓ 成功%s测试进程: PID=%d", action, pid));
                    ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                    JOptionPane.showMessageDialog(view, "PID: " + pid + " 已被成功" + action + "。", "操作成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to {} test process with PID {}", action.toLowerCase(), pid, e);
                    appendClientLog(String.format("✗ %s测试进程失败: PID=%d, 错误=%s", action, pid, e.getMessage()));
                    JOptionPane.showMessageDialog(view, action + " PID: " + pid + " 失败: \n" + e.getMessage(), "操作失败", JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.getStopTestButton().setEnabled(true);
                }
            }
        }.execute();
    }

    private void addTestToTable(ClientTestResult result) {
        DefaultTableModel model = (DefaultTableModel) view.getClientProcessesTable().getModel();
        String testId = result.getTestId().substring(0, 8); // Short ID
        String target = result.getConfiguration().getTargetHost() + ":" + result.getConfiguration().getTargetPort();
        String status = result.isSuccess() ? "完成" : "失败";
        String resultInfo = result.isSuccess() ? 
                (result.getClientJsonResult() != null && result.getClientJsonResult().getEnd() != null &&
                 result.getClientJsonResult().getEnd().getSumReceived() != null ?
                 String.format("%.2f Mbps", result.getClientJsonResult().getEnd().getSumReceived().getBitsPerSecond() / 1_000_000) :
                 "成功") :
                result.getErrorMessage();
        
        model.addRow(new Object[]{testId, target, status, resultInfo});
        view.getClientProcessesTable().revalidate();
        view.getClientProcessesTable().repaint();
    }

    private void updateTestTable(List<ClientTestInstance> instances) {
        log.info("Attempting to update test table with {} instances.", instances.size());
        DefaultTableModel model = (DefaultTableModel) view.getClientProcessesTable().getModel();
        
        model.setRowCount(0); // Clear table

        for (ClientTestInstance instance : instances) {
            model.addRow(new Object[]{
                    instance.getPid(),
                    instance.getStatus() != null ? instance.getStatus().toString() : "UNKNOWN",
                    instance.getTargetHost() != null ? instance.getTargetHost() : "N/A",
                    instance.getTargetPort(),
                    instance.getCommandLine() != null ? instance.getCommandLine() : "N/A"
            });
        }
        
        view.getClientProcessesTable().revalidate();
        view.getClientProcessesTable().repaint();
    }

    private void clearTestTable() {
        DefaultTableModel model = (DefaultTableModel) view.getClientProcessesTable().getModel();
        model.setRowCount(0);
    }
}

