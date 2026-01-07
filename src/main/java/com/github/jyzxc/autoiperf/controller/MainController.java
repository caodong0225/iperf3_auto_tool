package com.github.jyzxc.autoiperf.controller;

import com.github.jyzxc.autoiperf.model.IperfResult;
import com.github.jyzxc.autoiperf.model.TestConfig;
import com.github.jyzxc.autoiperf.model.TestResult;
import com.github.jyzxc.autoiperf.service.ResultPersistenceService;
import com.github.jyzxc.autoiperf.service.TestExecutionService;
import com.github.jyzxc.autoiperf.ui.ConfigPanel;
import com.github.jyzxc.autoiperf.ui.MainFrame;
import com.github.jyzxc.autoiperf.ui.RemoteMachinePanel;
import com.github.jyzxc.autoiperf.ui.TestParametersPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.UUID;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final MainFrame mainFrame;
    private final RemoteMachinePanel clientMachinePanel;
    private final RemoteMachinePanel serverMachinePanel;
    private final TestParametersPanel testParametersPanel;
    private final JButton testConnectivityButton;
    private final JButton startTestButton;

    private final TestExecutionService testExecutionService;
    private final ResultPersistenceService resultPersistenceService;


    public MainController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        
        ConfigPanel configPanel = mainFrame.getConfigPanel();
        this.clientMachinePanel = configPanel.getClientMachinePanel();
        this.serverMachinePanel = configPanel.getServerMachinePanel();
        this.testConnectivityButton = configPanel.getTestConnectivityButton();
        this.startTestButton = configPanel.getStartTestButton();
        this.testParametersPanel = configPanel.getTestParametersPanel();

        this.testExecutionService = new TestExecutionService();
        this.resultPersistenceService = new ResultPersistenceService();

        addListeners();
    }

    private void addListeners() {
        testConnectivityButton.addActionListener(e -> handleTestConnectivity());
        startTestButton.addActionListener(e -> handleStartTest());

        clientMachinePanel.addConnectionStateListener(this::disableStartTestButton);
        serverMachinePanel.addConnectionStateListener(this::disableStartTestButton);
        
        testParametersPanel.getPortSpinner().addChangeListener(e -> disableStartTestButton(false));
        testParametersPanel.getProtocolComboBox().addActionListener(e -> disableStartTestButton(false));
        testParametersPanel.getDurationSpinner().addChangeListener(e -> disableStartTestButton(false));
        testParametersPanel.getPacketSizeComboBox().addActionListener(e -> disableStartTestButton(false));
    }
    
    private void handleStartTest() {
        log.info("'Start Test' button clicked.");
        
        TestConfig config = buildTestConfig();
        if (config == null) return;

        setAllInputsEnabled(false);
        mainFrame.getStatusBar().setStatus("测试正在进行中...");
        mainFrame.getResultsPanel().setClientResultText("正在执行测试...");
        mainFrame.getResultsPanel().setServerResultText("等待客户端完成...");

        new SwingWorker<TestResult, Void>() {
            @Override
            protected TestResult doInBackground() throws Exception {
                String serverContext = null;
                IperfResult clientResult = null;
                IperfResult serverResult = null;
                boolean success = false;
                String errorMessage = null;

                try {
                    // 1. Start server
                    serverContext = testExecutionService.startServer(serverMachinePanel.getSshService(), config);
                    String[] contextParts = serverContext.split(";");
                    String serverTempFile = contextParts[0];
                    String serverPid = contextParts[1];
                    
                    // 2. Execute client
                    clientResult = testExecutionService.executeClient(clientMachinePanel.getSshService(), config);

                    // 3. Collect server result
                    serverResult = testExecutionService.collectServerResult(serverMachinePanel.getSshService(), serverTempFile);

                    // 4. Cleanup
                    testExecutionService.cleanupServer(serverMachinePanel.getSshService(), serverTempFile, serverPid);
                    success = true;
                } catch (Exception e) {
                    log.error("Test execution failed.", e);
                    errorMessage = e.getMessage();
                    // Attempt cleanup even on failure
                    if (serverContext != null) {
                        String[] contextParts = serverContext.split(";");
                        testExecutionService.cleanupServer(serverMachinePanel.getSshService(), contextParts[0], contextParts[1]);
                    }
                }

                return TestResult.builder()
                        .testId(UUID.randomUUID().toString())
                        .testTimestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .configuration(config)
                        .clientResult(clientResult)
                        .serverResult(serverResult)
                        .success(success)
                        .errorMessage(errorMessage)
                        .build();
            }

            @Override
            protected void done() {
                try {
                    TestResult finalResult = get();
                    displayResults(finalResult);
                    resultPersistenceService.saveResult(finalResult);
                    mainFrame.getStatusBar().setStatus("测试完成。");
                } catch (Exception e) {
                    log.error("An unexpected error occurred in the test worker's done() method.", e);
                    mainFrame.getResultsPanel().setClientResultText("测试执行期间发生意外错误:\n" + e.getMessage());
                    mainFrame.getStatusBar().setStatus("测试失败。");
                } finally {
                    setAllInputsEnabled(true);
                    startTestButton.setEnabled(false); // Force re-test of connectivity
                }
            }
        }.execute();
    }
    
    private void displayResults(TestResult result) {
        if (!result.isSuccess()) {
            mainFrame.getResultsPanel().setClientResultText("测试失败:\n" + result.getErrorMessage());
            mainFrame.getResultsPanel().setServerResultText("");
            return;
        }

        // Client Results
        IperfResult clientRes = result.getClientResult();
        double senderRate = clientRes.getEnd().getSumSent().getBitsPerSecond() / 1_000_000;
        double senderCpu = clientRes.getEnd().getCpuUtilizationPercent().getHostTotal();
        String clientText = String.format("发送速率: %.2f Mbps\n发送端 CPU: %.2f %%\n", senderRate, senderCpu);
        mainFrame.getResultsPanel().setClientResultText(clientText);

        // Server Results
        IperfResult serverRes = result.getServerResult();
        double receiverRate = serverRes.getEnd().getSumReceived().getBitsPerSecond() / 1_000_000;
        double receiverCpu = serverRes.getEnd().getCpuUtilizationPercent().getHostTotal();
        String serverText = String.format("接收速率: %.2f Mbps\n接收端 CPU: %.2f %%\n", receiverRate, receiverCpu);
        mainFrame.getResultsPanel().setServerResultText(serverText);
    }
    
    private TestConfig buildTestConfig() {
        try {
            return TestConfig.builder()
                .clientHost(clientMachinePanel.getHost())
                .serverHost(serverMachinePanel.getHost())
                .clientBindAddress(clientMachinePanel.getSelectedNicIp())
                .serverBindAddress(serverMachinePanel.getSelectedNicIp())
                .testPort((Integer) testParametersPanel.getPortSpinner().getValue())
                .duration((Integer) testParametersPanel.getDurationSpinner().getValue())
                .protocol((String) testParametersPanel.getProtocolComboBox().getSelectedItem())
                .packetSize((String) testParametersPanel.getPacketSizeComboBox().getSelectedItem())
                .build();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "无法构建测试配置，请检查所有参数是否选择正确。", "配置错误", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void setAllInputsEnabled(boolean enabled) {
        clientMachinePanel.setPanelEnabled(enabled);
        serverMachinePanel.setPanelEnabled(enabled);
        testParametersPanel.setEnabled(enabled);
        testConnectivityButton.setEnabled(enabled);
        startTestButton.setEnabled(enabled);
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
                return clientMachinePanel.getNetworkService().ping(clientSelectedIp, serverSelectedIp);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    log.debug("Ping result: {}", result);
                    
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