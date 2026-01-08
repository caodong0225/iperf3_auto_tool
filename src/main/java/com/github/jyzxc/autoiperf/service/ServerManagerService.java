package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.IperfServerInstance;
import com.github.jyzxc.autoiperf.model.PortInUseByIperfException;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the business logic for managing iperf3 server instances.
 */
public class ServerManagerService {

    private static final Logger log = LoggerFactory.getLogger(ServerManagerService.class);

    public IperfServerInstance startServer(SshService sshService, String host, int port, String bindAddress) throws Exception {
        log.info("Attempting to start iperf3 server on host {}, binding to {}:{}", host, bindAddress, port);

        // Ensure /tmp/iperf3 directory exists
        String mkdirCommand = "mkdir -p /tmp/iperf3";
        sshService.executeCommand(mkdirCommand, 3000);
        
        String remoteLogFile = String.format("/tmp/iperf3/server-%s.json", UUID.randomUUID());

        // Use a more reliable method to start iperf3 and get PID
        // The command uses nohup and redirects output to ensure it doesn't block
        String command = String.format(
                "nohup iperf3 -s -p %d -B %s -1 -J --logfile %s > /dev/null 2>&1 & echo $!",
                port,
                bindAddress,
                remoteLogFile
        );

        log.debug("Executing remote command: {}", command);
        String output = null;
        try {
            // Use shorter timeout for the initial command (5 seconds should be enough)
            output = sshService.executeCommand(command, 5000);
            log.info("Command output received: '{}'", output);
        } catch (Exception e) {
            log.error("Failed to execute start command: {}", e.getMessage(), e);
            throw new Exception("Failed to execute iperf3 start command: " + e.getMessage());
        }
        
        if (output == null || output.trim().isEmpty()) {
            log.error("Command returned null or empty output");
            throw new Exception("Failed to start iperf3 server: command returned empty output");
        }
        
        // Extract PID from output (may contain newlines or other text)
        String pidStr = output.trim().replaceAll("[^0-9]", "");
        if (pidStr.isEmpty() || !pidStr.matches("\\d+")) {
            log.error("Failed to extract valid PID from output: '{}'", output);
            // Attempt to read the log file for a more specific error
            try {
                String errorLog = sshService.executeCommand("cat " + remoteLogFile, 2000);
                sshService.executeCommand("rm " + remoteLogFile, 2000); // Cleanup
                throw new Exception("Failed to get PID for iperf3. Output: '" + output + "'. Error log: " + errorLog);
            } catch (Exception e) {
                throw new Exception("Failed to get PID for iperf3. Output: '" + output + "'. Could not read error log: " + e.getMessage());
            }
        }

        log.info("iPerf3 server process created with PID: {}. Verifying startup...", pidStr);

        // Polling mechanism to verify server is actually ready and not in an error state.
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds timeout (increased from 3s)
        int pollCount = 0;

        while (System.currentTimeMillis() - startTime < timeout) {
            pollCount++;
            Thread.sleep(300); // Poll every 300ms
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("Polling attempt {} ({}ms elapsed)...", pollCount, elapsed);

            // Check if the process is still alive (with timeout)
            String psCheckOutput = null;
            try {
                psCheckOutput = sshService.executeCommand("ps -p " + pidStr, 2000);
            } catch (Exception e) {
                log.warn("Failed to check process status: {}", e.getMessage());
                psCheckOutput = "";
            }
            
            if (psCheckOutput == null || !psCheckOutput.contains(pidStr)) {
                // Process died. Check the log file for the reason.
                log.warn("Process {} is not running. Checking error log...", pidStr);
                String errorLog = null;
                try {
                    errorLog = sshService.executeCommand("cat " + remoteLogFile, 2000);
                    log.debug("Error log content: {}", errorLog);
                } catch (Exception e) {
                    log.warn("Could not read error log: {}", e.getMessage());
                }
                
                // Cleanup log file
                try {
                    sshService.executeCommand("rm " + remoteLogFile, 2000);
                } catch (Exception e) {
                    log.debug("Could not remove log file: {}", e.getMessage());
                }
                
                // Check for specific error types
                if (errorLog != null && errorLog.contains("\"error\"")) {
                    String errorMessage = extractErrorFromJson(errorLog);
                    log.error("iPerf3 server failed to start: {}", errorMessage);
                    
                    // Check if it's a port in use error
                    if (errorMessage.contains("Address already in use") || 
                        errorMessage.contains("unable to start listener")) {
                        // Find which process is using the port
                        PortInfo portInfo = findPortOwner(sshService, port);
                        if (portInfo != null) {
                            log.info("Port {} is in use by process '{}' (PID: {})", 
                                    port, portInfo.processName, portInfo.pid);
                            throw new PortInUseByIperfException(
                                    String.format("端口 %d 已被进程 '%s' (PID: %s) 占用", 
                                            port, portInfo.processName, portInfo.pid),
                                    portInfo.processName,
                                    portInfo.pid);
                        } else {
                            throw new PortInUseByIperfException(
                                    String.format("端口 %d 已被占用，但无法确定占用进程", port),
                                    "unknown",
                                    "unknown");
                        }
                    }
                    
                    // Other errors
                    throw new Exception("iPerf3 server failed to start: " + errorMessage);
                }
                
                throw new Exception("iPerf3 process with PID " + pidStr + " died unexpectedly.");
            }
            
            // Check if the server is listening on the port using multiple methods
            // Method 1: ss command
            String listenCheckCommand = String.format("ss -tlpn | grep ':%d'", port);
            String listenCheckOutput = null;
            try {
                listenCheckOutput = sshService.executeCommand(listenCheckCommand, 2000);
                log.debug("ss command output for port {}: {}", port, listenCheckOutput);
            } catch (Exception e) {
                log.debug("ss command failed or timed out: {}", e.getMessage());
            }
            
            // Method 2: netstat as fallback
            boolean isListening = false;
            if (listenCheckOutput != null && (listenCheckOutput.contains(pidStr) || listenCheckOutput.contains(":" + port))) {
                isListening = true;
                log.debug("Port {} is listening (found via ss command)", port);
            } else {
                // Try netstat as alternative
                String netstatCommand = String.format("netstat -tlnp 2>/dev/null | grep ':%d' || netstat -tln 2>/dev/null | grep ':%d'", port, port);
                String netstatOutput = null;
                try {
                    netstatOutput = sshService.executeCommand(netstatCommand, 2000);
                    log.debug("netstat command output for port {}: {}", port, netstatOutput);
                } catch (Exception e) {
                    log.debug("netstat command failed or timed out: {}", e.getMessage());
                }
                if (netstatOutput != null && netstatOutput.contains(":" + port)) {
                    isListening = true;
                    log.debug("Port {} is listening (found via netstat command)", port);
                }
            }
            
            // If process is alive and we've waited at least 500ms, consider it successful
            // (iperf3 server starts quickly, and if process is alive, it's likely working)
            if (isListening || elapsed >= 500) {
                // Double-check: verify process is still running
                String finalPsCheck = null;
                try {
                    finalPsCheck = sshService.executeCommand("ps -p " + pidStr, 2000);
                } catch (Exception e) {
                    log.warn("Final process check failed: {}", e.getMessage());
                    finalPsCheck = "";
                }
                if (finalPsCheck != null && finalPsCheck.contains(pidStr)) {
                    log.info("Verified: PID {} is running on port {}. Server started successfully. (elapsed: {}ms)", 
                            pidStr, port, elapsed);
                    // Build command line for display
                    String cmdLine = String.format("iperf3 -s -p %d -B %s", port, bindAddress);
                    // Success condition
                    return IperfServerInstance.builder()
                            .instanceId(UUID.randomUUID().toString())
                            .remoteHost(host)
                            .boundIp(bindAddress)
                            .listeningPort(port)
                            .pid(Integer.parseInt(pidStr))
                            .commandLine(cmdLine)
                            .status(IperfServerInstance.ServerStatus.RUNNING)
                            .build();
                }
            }
        }
        
        // If we reach here, it's a timeout.
        log.error("Server startup verification timed out after {}ms ({} polling attempts).", timeout, pollCount);
        // Check if process is still alive before killing
        String finalCheck = null;
        try {
            finalCheck = sshService.executeCommand("ps -p " + pidStr, 2000);
        } catch (Exception e) {
            log.warn("Final process check failed: {}", e.getMessage());
        }
        if (finalCheck != null && finalCheck.contains(pidStr)) {
            log.warn("Process {} is still alive but verification timed out. Assuming success and returning instance.", pidStr);
            // Process is alive, assume it's working even if we couldn't verify the port
            String cmdLine = String.format("iperf3 -s -p %d -B %s", port, bindAddress);
            return IperfServerInstance.builder()
                    .instanceId(UUID.randomUUID().toString())
                    .remoteHost(host)
                    .boundIp(bindAddress)
                    .listeningPort(port)
                    .pid(Integer.parseInt(pidStr))
                    .commandLine(cmdLine)
                    .status(IperfServerInstance.ServerStatus.RUNNING)
                    .build();
        }
        
        // Process is dead, cleanup and throw error
        try {
            sshService.executeCommand("kill " + pidStr + " 2>/dev/null || true", 2000);
            sshService.executeCommand("rm " + remoteLogFile + " 2>/dev/null || true", 2000);
        } catch (Exception e) {
            log.warn("Cleanup commands failed: {}", e.getMessage());
        }
        throw new Exception("iPerf3 server startup timed out. Process verification failed.");
    }

    private String extractErrorFromJson(String json) {
        try {
            Pattern pattern = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Could not parse error from JSON, returning raw content.", e);
        }
        return json;
    }
    
    /**
     * Information about a process using a port.
     */
    private static class PortInfo {
        final String pid;
        final String processName;
        
        PortInfo(String pid, String processName) {
            this.pid = pid;
            this.processName = processName;
        }
    }
    
    /**
     * Finds which process is using the specified port.
     * @param sshService SSH service to execute commands
     * @param port The port number to check
     * @return PortInfo with PID and process name, or null if not found
     */
    private PortInfo findPortOwner(SshService sshService, int port) {
        try {
            // Try ss command first (more modern and reliable)
            String ssCommand = String.format("ss -tlpn | grep ':%d'", port);
            String ssOutput = null;
            try {
                ssOutput = sshService.executeCommand(ssCommand, 3000);
                log.debug("ss output for port {}: {}", port, ssOutput);
            } catch (Exception e) {
                log.debug("ss command failed: {}", e.getMessage());
            }
            
            if (ssOutput != null && !ssOutput.trim().isEmpty()) {
                // Parse ss output: LISTEN 0 128 0.0.0.0:5201 0.0.0.0:* users:(("iperf3",pid=2161,fd=3))
                Pattern ssPattern = Pattern.compile("pid=(\\d+).*?\"([^\"]+)\"");
                Matcher ssMatcher = ssPattern.matcher(ssOutput);
                if (ssMatcher.find()) {
                    String pid = ssMatcher.group(1);
                    String processName = ssMatcher.group(2);
                    log.info("Found port owner via ss: PID={}, Process={}", pid, processName);
                    return new PortInfo(pid, processName);
                }
                
                // Alternative pattern: users:(("iperf3",pid=2161,fd=3))
                Pattern ssPattern2 = Pattern.compile("\"([^\"]+)\".*?pid=(\\d+)");
                Matcher ssMatcher2 = ssPattern2.matcher(ssOutput);
                if (ssMatcher2.find()) {
                    String processName = ssMatcher2.group(1);
                    String pid = ssMatcher2.group(2);
                    log.info("Found port owner via ss (pattern 2): PID={}, Process={}", pid, processName);
                    return new PortInfo(pid, processName);
                }
            }
            
            // Fallback to netstat
            String netstatCommand = String.format("netstat -tlnp 2>/dev/null | grep ':%d'", port);
            String netstatOutput = null;
            try {
                netstatOutput = sshService.executeCommand(netstatCommand, 3000);
                log.debug("netstat output for port {}: {}", port, netstatOutput);
            } catch (Exception e) {
                log.debug("netstat command failed: {}", e.getMessage());
            }
            
            if (netstatOutput != null && !netstatOutput.trim().isEmpty()) {
                // Parse netstat output: tcp 0 0 0.0.0.0:5201 0.0.0.0:* LISTEN 2161/iperf3
                Pattern netstatPattern = Pattern.compile("(\\d+)/([^\\s]+)");
                Matcher netstatMatcher = netstatPattern.matcher(netstatOutput);
                if (netstatMatcher.find()) {
                    String pid = netstatMatcher.group(1);
                    String processName = netstatMatcher.group(2);
                    log.info("Found port owner via netstat: PID={}, Process={}", pid, processName);
                    return new PortInfo(pid, processName);
                }
            }
            
            log.warn("Could not determine which process is using port {}", port);
            return null;
        } catch (Exception e) {
            log.error("Error finding port owner: {}", e.getMessage(), e);
            return null;
        }
    }


        public List<IperfServerInstance> discoverRunningInstances(SshService sshService, String host) throws Exception {
            log.info("Discovering running iperf3 instances on host: {}", host);
            List<IperfServerInstance> discoveredInstances = new ArrayList<>();
    
            String command = "pgrep -af \"iperf3\"";
            String output = sshService.executeCommand(command);
    
            if (output == null || output.trim().isEmpty() || output.toLowerCase().contains("error")) {
                log.info("No running iperf3 processes found or pgrep failed on host: {}", host);
                return discoveredInstances;
            }
    
            Pattern portPattern = Pattern.compile("-p\\s+(\\d+)");
            Pattern bindPattern = Pattern.compile("-B\\s+([\\d.]+)");
    
            String[] lines = output.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || !(line.contains(" -s") || line.contains(" --server"))) {
                    continue; // Skip non-server processes
                }
    
                String[] parts = line.split("\\s+", 2);
                if (parts.length < 2) continue;
    
                try {
                    int pid = Integer.parseInt(parts[0]);
                    String cmdLine = parts[1];
    
                    // Find port
                    Matcher portMatcher = portPattern.matcher(cmdLine);
                    int port = portMatcher.find() ? Integer.parseInt(portMatcher.group(1)) : 5201; // Default iperf3 port
    
                    // Find bind address
                    Matcher bindMatcher = bindPattern.matcher(cmdLine);
                    String bindAddress = bindMatcher.find() ? bindMatcher.group(1) : "0.0.0.0"; // Default bind address
    
                    log.info("Discovered iperf3 server instance -> PID: {}, Port: {}, Bind Address: {}", pid, port, bindAddress);
    
                    IperfServerInstance instance = IperfServerInstance.builder()
                            .instanceId(UUID.randomUUID().toString())
                            .remoteHost(host)
                            .pid(pid)
                            .commandLine(cmdLine)
                            .listeningPort(port)
                            .boundIp(bindAddress)
                            .status(IperfServerInstance.ServerStatus.RUNNING)
                            .build();
                    discoveredInstances.add(instance);
    
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse PID from pgrep output line: '{}'", line, e);
                }
            }
                    return discoveredInstances;
                }
            
                public void stopServer(SshService sshService, int pid) throws Exception {
                    log.info("Attempting to gracefully stop process with PID: {}", pid);
                    String command = "kill " + pid;
                    sshService.executeCommand(command);
                    // We don't have a reliable way to check for command success here without more complex parsing,
                    // but if it fails, executeCommand will likely throw an exception or return an error string.
                    log.info("Sent SIGTERM signal to PID: {}", pid);
                }
            
                public void killServer(SshService sshService, int pid) throws Exception {
                    log.info("Attempting to forcefully kill process with PID: {}", pid);
                    String command = "kill -9 " + pid;
                    sshService.executeCommand(command);
                    log.info("Sent SIGKILL signal to PID: {}", pid);
                }
            }
            
