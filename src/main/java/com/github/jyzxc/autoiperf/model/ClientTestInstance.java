package com.github.jyzxc.autoiperf.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a running iperf3 client test instance on a remote machine.
 */
@Data
@Builder
public class ClientTestInstance {
    private String instanceId; // UUID
    private String remoteHost; // The client machine host
    private int pid; // Process ID
    private String targetHost; // Target server IP
    private int targetPort; // Target server port
    private String commandLine; // The full command line of the process
    private TestStatus status;

    public enum TestStatus {
        RUNNING,
        COMPLETED,
        STOPPED,
        ERROR,
        KILLED
    }
}

