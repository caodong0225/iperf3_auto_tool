package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.sshtool.SshService;
import com.jcraft.jsch.JSchException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NetworkService {

    private final SshService sshService;

    public NetworkService(SshService sshService) {
        this.sshService = sshService;
    }

    public List<String> getRemoteIpAddresses() throws JSchException {
        // Regex to find IPv4 addresses, ignoring loopback (127.0.0.1) and link-local (169.254...)
        // and virtual/docker interfaces.
        final Pattern ipPattern = Pattern.compile("inet (?!127\\.0\\.0\\.1|169\\.254\\.)(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");

        String output = sshService.executeCommand("ip -4 addr");

        return Arrays.stream(output.split("\n"))
                .map(ipPattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Executes a ping command from the connected machine to a destination IP, binding to a specific source IP.
     * @param sourceIp The source IP address on the client machine to ping from.
     * @param destinationIp The IP address to ping.
     * @return A string containing the raw output of the ping command.
     * @throws JSchException if the SSH command fails.
     */
    public String ping(String sourceIp, String destinationIp) throws JSchException {
        // -I <interface_address>: bind to a specific source IP. Crucial for multi-homed machines.
        // -c 4: send 4 packets. A common choice for a quick test.
        // -W 2: wait 2 seconds for a response.
        String command = String.format("ping -I %s -c 4 -W 2 %s", sourceIp, destinationIp);
        return sshService.executeCommand(command);
    }
}
