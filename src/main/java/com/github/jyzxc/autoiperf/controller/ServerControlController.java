package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.model.IperfServerInstance;
import com.github.jyzxc.autoiperf.service.ServerManagerService;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import com.github.jyzxc.autoiperf.ui.ServerControlPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class ServerControlController {

    private static final Logger log = LoggerFactory.getLogger(ServerControlController.class);

    private final ServerControlPanel view;
    private final ServerManagerService service;

    public ServerControlController(ServerControlPanel view, ServerManagerService service) {
        this.view = view;
        this.service = service;
        addListeners();
    }

    private void addListeners() {
        view.getStartServerButton().addActionListener(e -> handleStartServer());
        view.getRemoteMachinePanel().addConnectionStateListener(this::handleConnectionStateChange);
        view.getStopServerButton().addActionListener(e -> handleStopOrKillServer(false));
        view.getKillServerButton().addActionListener(e -> handleStopOrKillServer(true));
    }

    private void handleConnectionStateChange(boolean isConnected) {
        if (isConnected) {
            log.info("Connection established, discovering running instances...");
            handleDiscoverInstances();
        } else {
            log.info("Connection lost, clearing instance table.");
            clearInstanceTable();
        }
    }

    private void handleDiscoverInstances() {
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
                    });
                } catch (Exception e) {
                    log.error("Failed to discover running iperf3 instances.", e);
                    JOptionPane.showMessageDialog(view, "发现服务实例失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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
                    addInstanceToTable(instance);
                    JOptionPane.showMessageDialog(view, "iPerf3 服务已成功启动！\nPID: " + instance.getPid(), "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to start iperf3 server.", e);
                    JOptionPane.showMessageDialog(view, "启动服务失败: \n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    view.getStartServerButton().setEnabled(true);
                    view.getStartServerButton().setText("开启新服务");
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
                    ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                    JOptionPane.showMessageDialog(view, "PID: " + pid + " 已被成功" + action + "。", "操作成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    log.error("Failed to {} process with PID {}", action.toLowerCase(), pid, e);
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
        model.addRow(new Object[]{
                instance.getPid(),
                instance.getStatus(),
                instance.getBoundIp(),
                instance.getListeningPort(),
                instance.getCommandLine()
        });
        view.getServerInstancesTable().revalidate();
        view.getServerInstancesTable().repaint();
    }

    private void updateInstanceTable(List<IperfServerInstance> instances) {
        log.debug("Attempting to update table with {} instances.", instances.size());
        DefaultTableModel model = (DefaultTableModel) view.getServerInstancesTable().getModel();
        
        log.debug("Table model row count before update: {}", model.getRowCount());
        model.setRowCount(0); // Clear table
        log.debug("Table model row count after clearing: {}", model.getRowCount());

        for (IperfServerInstance instance : instances) {
            log.debug("Adding instance to table: PID={}", instance.getPid());
            addInstanceToTable(instance);
        }
        
        log.debug("Table model row count after adding all instances: {}", model.getRowCount());
        
        // Force UI refresh
        view.getServerInstancesTable().revalidate();
        view.getServerInstancesTable().repaint();
    }

    private void clearInstanceTable() {
        DefaultTableModel model = (DefaultTableModel) view.getServerInstancesTable().getModel();
        model.setRowCount(0);
    }
}
