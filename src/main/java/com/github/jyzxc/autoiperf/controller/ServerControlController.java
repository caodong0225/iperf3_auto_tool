package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.model.IperfServerInstance;
import com.github.jyzxc.autoiperf.service.ServerManagerService;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import com.github.jyzxc.autoiperf.ui.ServerControlPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

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

        // Disable button to prevent double-clicking
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
    
    private void addInstanceToTable(IperfServerInstance instance) {
        DefaultTableModel model = (DefaultTableModel) view.getServerInstancesTable().getModel();
        model.addRow(new Object[]{
                instance.getRemoteHost(),
                instance.getBoundIp(),
                instance.getListeningPort(),
                instance.getPid(),
                instance.getStatus()
        });
    }
}
