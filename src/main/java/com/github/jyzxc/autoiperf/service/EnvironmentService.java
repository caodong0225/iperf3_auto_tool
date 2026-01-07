package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.sshtool.SshService;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentService {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentService.class);
    private final SshService sshService;

    public EnvironmentService(SshService sshService) {
        this.sshService = sshService;
    }

    /**
     * Checks if iperf3 is available on the remote machine, trying with sudo if necessary.
     * @param sudoPassword The password to use for sudo.
     * @return true if iperf3 is found, false otherwise.
     * @throws JSchException if there is an issue with the SSH connection.
     */
    public boolean checkIperf3Exists(String sudoPassword) throws JSchException {
        final String checkCommand = "command -v iperf3";

        // 1. Try without sudo
        log.info("Checking for iperf3 without sudo...");
        String result = sshService.executeCommand(checkCommand);
        if (result != null && result.contains("iperf3")) {
            log.info("Found iperf3 at: {}", result.trim());
            return true;
        }

        // 2. Try with sudo if the first attempt failed
        log.warn("iperf3 not found in user's path. Trying with sudo...");
        result = sshService.executeSudoCommand(checkCommand, sudoPassword);
        if (result != null && result.contains("iperf3")) {
            log.info("Found iperf3 with sudo at: {}", result.trim());
            return true;
        }

        log.error("iperf3 command not found on the remote host, even after trying with sudo.");
        return false;
    }
}
