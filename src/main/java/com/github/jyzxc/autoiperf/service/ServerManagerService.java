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
            throw new Exception("Failed to start iperf3 server. Check remote logs. Output: " + output);
        }

        String pidStr = output.trim();
        log.info("iPerf3 server started successfully on host {} with PID: {}", host, pidStr);

        return IperfServerInstance.builder()
                .instanceId(UUID.randomUUID().toString())
                .remoteHost(host)
                .boundIp(bindAddress)
                .listeningPort(port)
                .pid(Integer.parseInt(pidStr))
                .status(IperfServerInstance.ServerStatus.RUNNING)
                .build();
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
