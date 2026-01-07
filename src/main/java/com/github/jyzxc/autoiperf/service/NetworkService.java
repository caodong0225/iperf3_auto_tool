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
}
