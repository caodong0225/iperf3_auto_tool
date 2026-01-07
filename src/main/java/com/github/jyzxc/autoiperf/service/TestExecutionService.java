package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.IperfResult;
import com.github.jyzxc.autoiperf.model.TestConfig;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import com.github.jyzxc.autoiperf.model.PortInUseByIperfException;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionService.class);
    private final Gson gson = new Gson();

    public IperfResult executeClient(SshService clientSsh, TestConfig config) throws Exception {
        log.info("Executing client test...");
        String command = String.format("iperf3 -c %s -p %d -B %s -t %d -l %s -J",
                config.getServerBindAddress(),
                config.getTestPort(),
                config.getClientBindAddress(),
                config.getDuration(),
                config.getPacketSize()
        );

        log.debug("Client command: {}", command);
        String clientJsonOutput = clientSsh.executeCommand(command);
        log.debug("Client raw JSON output: {}", clientJsonOutput);

        IperfResult result = gson.fromJson(clientJsonOutput, IperfResult.class);

        if (result != null && result.getError() != null) {
            throw new Exception("iperf3 client test failed: " + result.getError());
        }

        return result;
    }

    public String startServer(SshService serverSsh, TestConfig config) throws Exception {
        log.info("Starting iperf3 server in the background...");
        String tempFileCommand = "mktemp -p /tmp iperf-server.XXXXXX.json";
        String tempFilePath = serverSsh.executeCommand(tempFileCommand).trim();

        if (tempFilePath.isEmpty() || tempFilePath.contains("Error")) {
            log.error("Failed to create temp file on server: {}", tempFilePath);
            throw new Exception("Failed to create temp file on server. Check /tmp directory permissions.");
        }
        log.info("Server temp file created at: {}", tempFilePath);

        String serverCommand = String.format("iperf3 -s -p %d -B %s -J --logfile %s & echo $!",
                config.getTestPort(),
                config.getServerBindAddress(),
                tempFilePath
        );
        log.debug("Server command: {}", serverCommand);
        String serverPid = serverSsh.executeCommand(serverCommand).trim();

        if (serverPid.isEmpty() || !serverPid.matches("\\d+")) {
             log.error("Failed to get a valid PID for the iperf3 server process. Output: {}", serverPid);
             throw new Exception("Failed to start iperf3 server or get its PID.");
        }
        log.info("iPerf3 server started with PID: {} on host: {}. Now verifying it is listening.", serverPid, config.getServerHost());

        // ** Polling mechanism to ensure server is ready **
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds
        boolean isReady = false;
        String checkListenCommand = String.format("ss -tlpn | grep ':%d'", config.getTestPort());
        String checkProcessCommand = "ps -p " + serverPid;

        while (System.currentTimeMillis() - startTime < timeout) {
            String processCheckOutput = serverSsh.executeCommand(checkProcessCommand);
            if (processCheckOutput == null || !processCheckOutput.contains(serverPid)) {
                log.error("iPerf server process with PID {} is no longer running. It likely crashed.", serverPid);
                isReady = false;
                break;
            }

            String listenCheckOutput = serverSsh.executeCommand(checkListenCommand);
            if (listenCheckOutput != null && !listenCheckOutput.trim().isEmpty() && listenCheckOutput.contains(String.valueOf(config.getTestPort()))) {
                log.info("Server is listening on port {}. Proceeding with test.", config.getTestPort());
                isReady = true;
                break;
            }
            Thread.sleep(200); // Poll every 200ms
        }

        if (!isReady) {
            try {
                log.error("Server did not start listening on port {} within {}ms.", config.getTestPort(), timeout);
                String errorMessage = "Server failed to start listening on port " + config.getTestPort() + " within the timeout period.";

                String serverJsonOutput = serverSsh.executeCommand("cat " + tempFilePath);
                log.debug("Server raw JSON output on failure: {}", serverJsonOutput);
                IperfResult result = gson.fromJson(serverJsonOutput, IperfResult.class);

                if (result != null && result.getError() != null) {
                    errorMessage = "iPerf server failed to start: " + result.getError();
                    log.error(errorMessage);

                    if (result.getError().contains("Address already in use")) {
                        String portCheckCmd = String.format("ss -tlpn | grep ':%d'", config.getTestPort());
                        String processInfo = serverSsh.executeCommand(portCheckCmd);
                        log.debug("Found process listening on port {}: {}", config.getTestPort(), processInfo);

                        Pattern pattern = Pattern.compile("users:\\(\\\"([^\\\"]+)\\\",pid=(\\d+),.*\\)");
                        Matcher matcher = pattern.matcher(processInfo);

                        if (matcher.find()) {
                            String processName = matcher.group(1);
                            String pid = matcher.group(2);
                            if ("iperf3".equalsIgnoreCase(processName)) {
                                String msg = String.format("Port %d is already in use by another iperf3 process (PID: %s).", config.getTestPort(), pid);
                                log.warn(msg);
                                throw new PortInUseByIperfException(msg, processName, pid);
                            } else {
                                errorMessage = String.format("Port %d is already in use by process '%s' (PID: %s).", config.getTestPort(), processName, pid);
                            }
                        }
                    }
                }
                throw new Exception(errorMessage);

            } catch (Exception finalException) {
                cleanupServer(serverSsh, tempFilePath, serverPid);
                throw finalException;
            }
        }

        return tempFilePath + ";" + serverPid;
    }

    public IperfResult collectServerResult(SshService serverSsh, String tempFilePath) throws Exception {
        log.info("Collecting server results from {}", tempFilePath);
        String serverJsonOutput = serverSsh.executeCommand("cat " + tempFilePath);
        log.debug("Server raw JSON output: {}", serverJsonOutput);
        IperfResult result = gson.fromJson(serverJsonOutput, IperfResult.class);
        if (result != null && result.getError() != null) {
            throw new Exception("iPerf server reported an error: " + result.getError());
        }
        return result;
    }

    public void killProcessByPid(SshService serverSsh, String pid) throws Exception {
        if (pid == null || pid.isEmpty()) {
            log.warn("Attempted to kill process with null or empty PID.");
            return;
        }
        log.info("Attempting to kill process with PID: {}", pid);
        serverSsh.executeCommand("kill " + pid);
        log.info("Sent kill signal to process with PID: {}", pid);
    }

    public void cleanupServer(SshService serverSsh, String tempFilePath, String pid) {
        log.info("Cleaning up server resources (PID: {}, File: {})", pid, tempFilePath);
        try {
            if (pid != null && !pid.isEmpty()) {
                // Check if the process exists before trying to kill it
                String checkProcessCommand = "ps -p " + pid;
                String processCheckOutput = serverSsh.executeCommand(checkProcessCommand);
                if (processCheckOutput != null && processCheckOutput.contains(pid)) {
                    serverSsh.executeCommand("kill " + pid);
                    log.info("Killed server process with PID: {}", pid);
                } else {
                    log.info("Server process with PID {} was already gone.", pid);
                }
            }
            if (tempFilePath != null && !tempFilePath.isEmpty()) {
                serverSsh.executeCommand("rm " + tempFilePath);
                log.info("Removed server temp file: {}", tempFilePath);
            }
        } catch (Exception e) {
            log.error("Error during server cleanup. Manual cleanup may be required.", e);
        }
    }
}
