package com.github.jyzxc.autoiperf.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a running iperf3 server instance on a remote machine.
 */
@Data
@Builder
public class IperfServerInstance {
    private String instanceId; // UUID
    private String remoteHost;
    private String boundIp;
    private int listeningPort;
    private int pid; // Process ID
    private String commandLine; // The full command line of the process
    private ServerStatus status;

    public enum ServerStatus {
        RUNNING,
        STOPPED,
        ERROR,
        KILLED
    }
}
