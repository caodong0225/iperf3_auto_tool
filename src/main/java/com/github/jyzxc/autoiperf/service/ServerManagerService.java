package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.IperfServerInstance;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

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
            // We should try to read the logfile to see the error from iperf3 itself here.
            // For now, we throw a generic exception.
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
}
