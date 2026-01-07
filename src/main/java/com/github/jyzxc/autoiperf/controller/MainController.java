package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.ui.ConfigPanel;
import com.github.jyzxc.autoiperf.ui.MainFrame;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final MainFrame mainFrame;
    private final RemoteMachinePanel clientMachinePanel;
    private final RemoteMachinePanel serverMachinePanel;
    private final JButton testConnectivityButton;
    private final JButton startTestButton;

    public MainController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        
        ConfigPanel configPanel = mainFrame.getConfigPanel();
        this.clientMachinePanel = configPanel.getClientMachinePanel();
        this.serverMachinePanel = configPanel.getServerMachinePanel();
        this.testConnectivityButton = configPanel.getTestConnectivityButton();
        this.startTestButton = configPanel.getStartTestButton();

        addListeners();
    }

    private void addListeners() {
        testConnectivityButton.addActionListener(e -> handleTestConnectivity());

        clientMachinePanel.addConnectionStateListener(this::onConnectionStateChanged);
        serverMachinePanel.addConnectionStateListener(this::onConnectionStateChanged);
    }

    private void onConnectionStateChanged(boolean isConnected) {
        startTestButton.setEnabled(false);
        log.debug("Start Test button disabled due to connection state change.");
    }

    private void handleTestConnectivity() {
        log.info("Test Connectivity button clicked.");

        if (!clientMachinePanel.isConnected() || !serverMachinePanel.isConnected()) {
            JOptionPane.showMessageDialog(mainFrame, "请先确保两台测试机都已成功连接。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serverSelectedIp = serverMachinePanel.getSelectedNicIp();
        if (serverSelectedIp == null || serverSelectedIp.startsWith("待") || serverSelectedIp.startsWith("未")) {
            JOptionPane.showMessageDialog(mainFrame, "请为测试机 B (服务端) 选择一个用于测试的网卡 IP。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        log.info("Pinging from client machine to server machine IP: {}", serverSelectedIp);
        mainFrame.getStatusBar().setStatus("正在从客户端 ping 服务端...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return clientMachinePanel.getNetworkService().ping(serverSelectedIp);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    log.debug("Ping result: {}", result);
                    
                    boolean isSuccess = result.contains("packets transmitted") && !result.contains(" 100% packet loss");
                    
                    if (isSuccess) {
                        JOptionPane.showMessageDialog(mainFrame, "连通性良好！\nPing 结果:\n" + result, "成功", JOptionPane.INFORMATION_MESSAGE);
                        startTestButton.setEnabled(true);
                        mainFrame.getStatusBar().setStatus("连通性测试通过。可以开始测试。");
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "无法从客户端 ping 通服务端。\nPing 结果:\n" + result, "连通性失败", JOptionPane.ERROR_MESSAGE);
                        startTestButton.setEnabled(false);
                        mainFrame.getStatusBar().setStatus("连通性测试失败。");
                    }
                } catch (Exception e) {
                    log.error("Ping execution failed.", e);
                    startTestButton.setEnabled(false);
                    mainFrame.getStatusBar().setStatus("连通性测试出错。");
                    clientMachinePanel.showDetailedErrorDialog(e);
                }
            }
        }.execute();
    }
}