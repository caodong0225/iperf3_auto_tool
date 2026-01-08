package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.IperfServerInstance;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is responsible for the business logic of managing iperf3 server instances on remote machines.
 */
public class ServerManagerService {

    private static final Logger log = LoggerFactory.getLogger(ServerManagerService.class);

    /**
     * Starts an iperf3 server instance on a remote machine.
     *
     * @param sshService  The connected SSH service for the target machine.
     * @param port        The port to listen on.
     * @param bindAddress The network interface IP to bind to.
     * @return An IperfServerInstance object representing the newly started process.
     * @throws Exception if the server fails to start.
     */
    public IperfServerInstance startServer(SshService sshService, int port, String bindAddress) throws Exception {
        // Core logic will be implemented in the next step.
        log.info("Attempting to start iperf3 server on port {} at bind address {}", port, bindAddress);
        
        // Placeholder implementation
        throw new UnsupportedOperationException("Server start logic not yet implemented.");
    }

}
