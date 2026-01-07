package com.github.jyzxc.autoiperf.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestConfig {
    private String clientHost;
    private String serverHost;
    private String clientBindAddress;
    private String serverBindAddress;
    private int testPort;
    private int duration;
    private String protocol;
    private String packetSize;
}
