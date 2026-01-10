package com.github.jyzxc.autoiperf.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the complete configuration for a client test.
 */
@Data
@Builder
public class ClientTestConfig {
    private SshProfile sourceMachineProfile; // The machine that will initiate the test
    private String sourceBindAddress;      // IP address to bind on the client side
    private String targetHost;              // Target server IP
    private int targetPort;                 // Target server port
    private int duration;                   // Test duration in seconds
    private String protocol;                // TCP or UDP
    private Integer parallelStreams;        // Number of parallel streams (optional)
    private Integer windowSize;             // TCP window size (optional)
    private Integer bandwidth;              // Bandwidth limit in bits/sec (optional)
    private Integer packetSize;             // Packet size in bytes (optional)
    private Boolean reverse;                // Reverse test (server sends, client receives)
    private Boolean bidirectionalTest;      // Bidirectional test (both send and receive)
    private String targetHost2;             // Second target host for bidirectional test (optional)
    private int targetPort2;                // Second target port for bidirectional test (optional)
}

