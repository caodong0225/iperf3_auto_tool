package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.IperfServerInstance;
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

        String remoteLogFile = String.format("/tmp/iperf-server-%s.json", UUID.randomUUID());

        String command = String.format(
                "iperf3 -s -p %d -B %s -J --logfile %s & echo $!",
                port,
                bindAddress,
                remoteLogFile
        );

        log.debug("Executing remote command: {}", command);
        String output = sshService.executeCommand(command);
        if (output == null || output.trim().isEmpty() || !output.trim().matches("\\d+")) {
            log.error("Failed to start iperf3 server or get a valid PID. Output: {}", output);
            // Attempt to read the log file for a more specific error
            String errorLog = sshService.executeCommand("cat " + remoteLogFile);
            sshService.executeCommand("rm " + remoteLogFile); // Cleanup
            throw new Exception("Failed to get PID for iperf3. Error log: " + errorLog);
        }

        String pidStr = output.trim();
        log.info("iPerf3 server process created with PID: {}. Verifying startup...", pidStr);

        // Polling mechanism to verify server is actually ready and not in an error state.
        long startTime = System.currentTimeMillis();
        long timeout = 3000; // 3 seconds timeout

        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(300); // Poll every 300ms

            // Check if the process is still alive
            String psCheckOutput = sshService.executeCommand("ps -p " + pidStr);
            if (!psCheckOutput.contains(pidStr)) {
                // Process died. Check the log file for the reason.
                String errorLog = sshService.executeCommand("cat " + remoteLogFile);
                sshService.executeCommand("rm " + remoteLogFile);
                if(errorLog != null && errorLog.contains("\"error\"")) {
                     throw new Exception("iPerf3 server failed to start: " + extractErrorFromJson(errorLog));
                }
                throw new Exception("iPerf3 process with PID " + pidStr + " died unexpectedly.");
            }
            
            // Check if the server is listening on the port. This is a more reliable check.
             String listenCheckCommand = String.format("ss -tlpn | grep ':%d'", port);
             String listenCheckOutput = sshService.executeCommand(listenCheckCommand);
             if (listenCheckOutput != null && listenCheckOutput.contains(pidStr)) {
                 log.info("Verified: PID {} is listening on port {}. Server started successfully.", pidStr, port);
                 // Success condition
                 return IperfServerInstance.builder()
                         .instanceId(UUID.randomUUID().toString())
                         .remoteHost(host)
                         .boundIp(bindAddress)
                         .listeningPort(port)
                         .pid(Integer.parseInt(pidStr))
                         .status(IperfServerInstance.ServerStatus.RUNNING)
                         .build();
             }
        }
        
        // If we reach here, it's a timeout.
        log.error("Server startup verification timed out after {}ms.", timeout);
        // Cleanup attempt
        sshService.executeCommand("kill " + pidStr);
        sshService.executeCommand("rm " + remoteLogFile);
        throw new Exception("iPerf3 server startup timed out. Process was killed.");
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


    public List<IperfServerInstance> discoverRunningInstances(SshService sshService, String host) throws Exception {
        log.info("Discovering running iperf3 instances on host: {}", host);
        List<IperfServerInstance> discoveredInstances = new ArrayList<>();

        // pgrep -af "iperf3 -s": Find processes whose command line matches "iperf3 -s"
        String command = "pgrep -af \"iperf3 -s\"";
        String output = sshService.executeCommand(command);

        if (output == null || output.trim().isEmpty() || output.toLowerCase().contains("error")) {
            log.info("No running iperf3 server instances found or pgrep failed on host: {}", host);
            return discoveredInstances;
        }

        // Regex to capture PID, -p port, and -B bindAddress from lines like:
        // 12345 iperf3 -s -p 5202 -B 192.168.1.100 -J --logfile /tmp/log.json
        Pattern pattern = Pattern.compile("(\\d+)\\s+iperf3 -s(?:(?!-c).)*?-p\\s+(\\d+)(?:(?!-c).)*?-B\\s+([\\d.]+)");

        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                try {
                    int pid = Integer.parseInt(matcher.group(1));
                    int port = Integer.parseInt(matcher.group(2));
                    String bindAddress = matcher.group(3);

                    log.info("Discovered iperf3 instance -> PID: {}, Port: {}, Bind Address: {}", pid, port, bindAddress);

                    IperfServerInstance instance = IperfServerInstance.builder()
                            .instanceId(UUID.randomUUID().toString()) // Generate a new UI-internal ID
                            .remoteHost(host)
                            .pid(pid)
                            .listeningPort(port)
                            .boundIp(bindAddress)
                            .status(IperfServerInstance.ServerStatus.RUNNING)
                            .build();
                    discoveredInstances.add(instance);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse PID or port from pgrep output line: '{}'", line, e);
                }
            } else {
                log.warn("pgrep output line did not match expected pattern: '{}'", line);
            }
        }
        return discoveredInstances;
    }
}
