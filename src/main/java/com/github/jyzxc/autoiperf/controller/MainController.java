package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.ui.ConfigPanel;
import com.github.jyzxc.autoiperf.ui.MainFrame;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import com.github.jyzxc.autoiperf.ui.TestParametersPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final MainFrame mainFrame;
    private final RemoteMachinePanel clientMachinePanel;
    private final RemoteMachinePanel serverMachinePanel;
    private final TestParametersPanel testParametersPanel;
    private final JButton testConnectivityButton;
    private final JButton startTestButton;

    public MainController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        
        ConfigPanel configPanel = mainFrame.getConfigPanel();
        this.clientMachinePanel = configPanel.getClientMachinePanel();
        this.serverMachinePanel = configPanel.getServerMachinePanel();
        this.testConnectivityButton = configPanel.getTestConnectivityButton();
        this.startTestButton = configPanel.getStartTestButton();
        
        // This is a bit of a workaround to get access to the params panel
        this.testParametersPanel = (TestParametersPanel) ((JPanel) configPanel.getComponent(1)).getComponent(0);

        addListeners();
    }

    private void addListeners() {
        testConnectivityButton.addActionListener(e -> handleTestConnectivity());

        // Any change in connection state or NIC selection should disable the start button
        clientMachinePanel.addConnectionStateListener(this::disableStartTestButton);
        serverMachinePanel.addConnectionStateListener(this::disableStartTestButton);
        
        // Any change in test parameters should also disable the start button
        testParametersPanel.getPortSpinner().addChangeListener(e -> disableStartTestButton(false));
        testParametersPanel.getProtocolComboBox().addActionListener(e -> disableStartTestButton(false));
        testParametersPanel.getDurationSpinner().addChangeListener(e -> disableStartTestButton(false));
        testParametersPanel.getPacketSizeComboBox().addActionListener(e -> disableStartTestButton(false));
    }

    private void disableStartTestButton(boolean connectionChanged) {
        if (startTestButton.isEnabled()) {
            log.info("A critical parameter was changed. Disabling 'Start Test' button. Please re-test connectivity.");
            mainFrame.getStatusBar().setStatus("参数已变更，请重新测试连通性。");
        }
        startTestButton.setEnabled(false);
    }

    private void handleTestConnectivity() {
        log.info("Test Connectivity button clicked.");

        if (!clientMachinePanel.isConnected() || !serverMachinePanel.isConnected()) {
            JOptionPane.showMessageDialog(mainFrame, "请先确保两台测试机都已成功连接。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String clientSelectedIp = clientMachinePanel.getSelectedNicIp();
        String serverSelectedIp = serverMachinePanel.getSelectedNicIp();

        if (clientSelectedIp == null || clientSelectedIp.startsWith("待") || clientSelectedIp.startsWith("未")) {
            JOptionPane.showMessageDialog(mainFrame, "请为测试机 A (客户端) 选择一个用于测试的网卡 IP。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (serverSelectedIp == null || serverSelectedIp.startsWith("待") || serverSelectedIp.startsWith("未")) {
            JOptionPane.showMessageDialog(mainFrame, "请为测试机 B (服务端) 选择一个用于测试的网卡 IP。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        log.info("Pinging from {} to {}", clientSelectedIp, serverSelectedIp);
        mainFrame.getStatusBar().setStatus(String.format("正在从 %s ping %s...", clientSelectedIp, serverSelectedIp));

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Use the client's service to execute the ping from the selected source IP
                return clientMachinePanel.getNetworkService().ping(clientSelectedIp, serverSelectedIp);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    log.debug("Ping result: {}", result);
                    
                    // Improved check for success, looking for "0% packet loss" is more reliable
                    boolean isSuccess = result.contains(" 0% packet loss") || (result.contains("packets transmitted") && !result.contains("100% packet loss"));
                    
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