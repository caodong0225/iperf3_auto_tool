package com.github.jyzxc.autoiperf.model;

public class PortInUseByIperfException extends Exception {
    private final String pid;
    private final String processName;

    public PortInUseByIperfException(String message, String processName, String pid) {
        super(message);
        this.processName = processName;
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public String getProcessName() {
        return processName;
    }
}
