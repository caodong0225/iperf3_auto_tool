package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.ClientTestConfig;
import com.github.jyzxc.autoiperf.model.ClientTestInstance;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the business logic for managing iperf3 client test instances.
 */
public class ClientManagerService {

    private static final Logger log = LoggerFactory.getLogger(ClientManagerService.class);

    /**
     * Start a client test as a background process.
     * @param sshService SSH service connected to the client machine
     * @param host The client machine host
     * @param config Test configuration
     * @return ClientTestInstance with PID
     * @throws Exception if the test fails to start
     */
    public ClientTestInstance startClientTest(SshService sshService, String host, ClientTestConfig config) throws Exception {
        log.info("Attempting to start iperf3 client test on host {}, target={}:{}", 
                host, config.getTargetHost(), config.getTargetPort());

        // Generate JSON file path on remote machine
        String testId = UUID.randomUUID().toString();
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String remoteJsonFile = String.format("/tmp/iperf3/test_%s_%s.json", timestamp, testId.substring(0, 8));

        // Ensure /tmp/iperf3 directory exists
        String mkdirCommand = "mkdir -p /tmp/iperf3";
        sshService.executeCommand(mkdirCommand, 3000);
        log.debug("Ensured /tmp/iperf3 directory exists");

        // Build iperf3 client command (includes --logfile for JSON output)
        String command = buildIperf3ClientCommand(config, remoteJsonFile);
        log.debug("Executing remote command: {}", command);

        // Start command in background and get PID
        // Use --logfile parameter to save JSON output directly (not affected by stdout/stderr redirection)
        // Redirect stdout/stderr to /dev/null to avoid cluttering terminal output
        String bgCommand = String.format("nohup %s > /dev/null 2>&1 & echo $!", command);
        String output = null;
        try {
            output = sshService.executeCommand(bgCommand, 5000);
            log.info("Command output received: '{}'", output);
        } catch (Exception e) {
            log.error("Failed to execute start command: {}", e.getMessage(), e);
            throw new Exception("Failed to execute iperf3 client command: " + e.getMessage());
        }

        if (output == null || output.trim().isEmpty()) {
            log.error("Command returned null or empty output");
            throw new Exception("Failed to start iperf3 client test: command returned empty output");
        }

        // Extract PID from output
        String pidStr = output.trim().replaceAll("[^0-9]", "");
        if (pidStr.isEmpty() || !pidStr.matches("\\d+")) {
            log.error("Failed to extract valid PID from output: '{}'", output);
            throw new Exception("Failed to get PID for iperf3 client. Output: '" + output + "'");
        }

        log.info("iPerf3 client test process created with PID: {}. Verifying startup...", pidStr);

        // Verify process is running
        Thread.sleep(300);
        String psCheckOutput = null;
        try {
            psCheckOutput = sshService.executeCommand("ps -p " + pidStr, 2000);
        } catch (Exception e) {
            log.warn("Failed to check process status: {}", e.getMessage());
            psCheckOutput = "";
        }

        if (psCheckOutput == null || !psCheckOutput.contains(pidStr)) {
            log.error("Process {} is not running after startup", pidStr);
            throw new Exception("iperf3 client process with PID " + pidStr + " failed to start or died immediately.");
        }

        // Build command line for display
        String cmdLine = buildIperf3ClientCommand(config, remoteJsonFile);

        log.info("Verified: PID {} is running. Client test started successfully.", pidStr);
        return ClientTestInstance.builder()
                .instanceId(testId)
                .remoteHost(host)
                .pid(Integer.parseInt(pidStr))
                .targetHost(config.getTargetHost())
                .targetPort(config.getTargetPort())
                .commandLine(cmdLine)
                .status(ClientTestInstance.TestStatus.RUNNING)
                .build();
    }

    /**
     * Build the iperf3 client command string based on configuration.
     * Uses --logfile parameter to save JSON output to file (more reliable than shell redirection).
     */
    private String buildIperf3ClientCommand(ClientTestConfig config, String jsonFilePath) {
        StringBuilder cmd = new StringBuilder("iperf3 -c ");
        cmd.append(config.getTargetHost());
        cmd.append(" -p ").append(config.getTargetPort());
        cmd.append(" -t ").append(config.getDuration());
        cmd.append(" -J"); // JSON output format
        cmd.append(" --logfile ").append(jsonFilePath); // Use --logfile to save JSON output directly

        // Protocol
        if ("UDP".equalsIgnoreCase(config.getProtocol())) {
            cmd.append(" -u");
            if (config.getBandwidth() != null) {
                cmd.append(" -b ").append(config.getBandwidth());
            }
            if (config.getPacketSize() != null) {
                cmd.append(" -l ").append(config.getPacketSize());
            }
        } else {
            // TCP options
            if (config.getWindowSize() != null) {
                cmd.append(" -w ").append(config.getWindowSize());
            }
            if (config.getBandwidth() != null) {
                cmd.append(" -b ").append(config.getBandwidth());
            }
        }

        // Parallel streams
        if (config.getParallelStreams() != null && config.getParallelStreams() > 1) {
            cmd.append(" -P ").append(config.getParallelStreams());
        }

        // Reverse test
        if (config.getReverse() != null && config.getReverse()) {
            cmd.append(" -R");
        }

        // Bind address
        if (config.getSourceBindAddress() != null && !config.getSourceBindAddress().isEmpty()) {
            cmd.append(" -B ").append(config.getSourceBindAddress());
        }

        return cmd.toString();
    }

    /**
     * Discover running iperf3 client processes on the remote machine.
     */
    public List<ClientTestInstance> discoverRunningClientTests(SshService sshService, String host) throws Exception {
        log.info("Discovering running iperf3 client processes on host: {}", host);
        List<ClientTestInstance> discoveredInstances = new ArrayList<>();

        String command = "pgrep -af \"iperf3\"";
        String output = sshService.executeCommand(command, 3000);

        if (output == null || output.trim().isEmpty() || output.toLowerCase().contains("error")) {
            log.info("No running iperf3 client processes found on host: {}", host);
            return discoveredInstances;
        }

        Pattern portPattern = Pattern.compile("-p\\s+(\\d+)");
        Pattern targetPattern = Pattern.compile("-c\\s+([\\d.]+)");

        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.contains(" -s") || line.contains(" --server")) {
                continue; // Skip server processes
            }

            // Check if it's a client process (contains -c)
            if (!line.contains(" -c") && !line.contains(" --client")) {
                continue;
            }

            String[] parts = line.split("\\s+", 2);
            if (parts.length < 2) continue;

            try {
                int pid = Integer.parseInt(parts[0]);
                String cmdLine = parts[1];

                // Find target host
                Matcher targetMatcher = targetPattern.matcher(cmdLine);
                String targetHost = targetMatcher.find() ? targetMatcher.group(1) : "unknown";

                // Find port
                Matcher portMatcher = portPattern.matcher(cmdLine);
                int port = portMatcher.find() ? Integer.parseInt(portMatcher.group(1)) : 5201;

                log.info("Discovered iperf3 client instance -> PID: {}, Target: {}:{}", pid, targetHost, port);

                ClientTestInstance instance = ClientTestInstance.builder()
                        .instanceId(UUID.randomUUID().toString())
                        .remoteHost(host)
                        .pid(pid)
                        .targetHost(targetHost)
                        .targetPort(port)
                        .commandLine(cmdLine)
                        .status(ClientTestInstance.TestStatus.RUNNING)
                        .build();
                discoveredInstances.add(instance);

            } catch (NumberFormatException e) {
                log.warn("Failed to parse PID from pgrep output line: '{}'", line, e);
            }
        }
        return discoveredInstances;
    }

    /**
     * Stop a running client test gracefully.
     */
    public void stopClientTest(SshService sshService, int pid) throws Exception {
        log.info("Attempting to gracefully stop client test process with PID: {}", pid);
        String command = "kill " + pid;
        sshService.executeCommand(command, 3000);
        log.info("Sent SIGTERM signal to PID: {}", pid);
    }

    /**
     * Forcefully kill a running client test.
     */
    public void killClientTest(SshService sshService, int pid) throws Exception {
        log.info("Attempting to forcefully kill client test process with PID: {}", pid);
        String command = "kill -9 " + pid;
        sshService.executeCommand(command, 3000);
        log.info("Sent SIGKILL signal to PID: {}", pid);
    }
}

