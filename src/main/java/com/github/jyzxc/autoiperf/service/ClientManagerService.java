package com.github.jyzxc.autoiperf.service;

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

