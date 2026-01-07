package com.github.jyzxc.autoiperf.sshtool;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SshService {

    private static final Logger log = LoggerFactory.getLogger(SshService.class);
    private Session session;

    public void connect(String username, String password, String host, int port) throws JSchException {
        if (session != null && session.isConnected()) {
            disconnect();
        }
        log.info("Connecting to ssh://{}@{}:{}", username, host, port);
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no"); // For simplicity, auto-accept host key
        session.connect(10000); // 10-second timeout
        log.info("SSH connection established to {}", host);
    }

    public String executeCommand(String command) throws JSchException {
        if (session == null || !session.isConnected()) {
            log.error("Cannot execute command. Session is not connected.");
            throw new JSchException("Session is not connected.");
        }

        log.debug("Executing remote command: {}", command);
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
            channel.setOutputStream(outputBuffer);
            channel.setErrStream(errorBuffer);

            channel.connect();

            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Command execution was interrupted.", e);
                    return "Command execution interrupted.";
                }
            }

            String error = new String(errorBuffer.toByteArray());
            String output = new String(outputBuffer.toByteArray());

            if (!error.isEmpty()) {
                log.error("Remote command execution failed with error: {}", error);
                // Return error as part of the string to ensure it's visible
                return "Error: " + error;
            }

            log.debug("Remote command output: {}", output);
            return output;

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public String executeSudoCommand(String command, String password) throws JSchException {
        if (session == null || !session.isConnected()) {
            log.error("Cannot execute sudo command. Session is not connected.");
            throw new JSchException("Session is not connected.");
        }

        // -S: read password from stdin, -p '': suppress sudo's own password prompt
        String sudoCommand = "sudo -S -p '' " + command;
        log.debug("Executing remote sudo command...");

        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(sudoCommand);

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
            channel.setOutputStream(outputBuffer);
            channel.setErrStream(errorBuffer);

            // Write password to the command's standard input
            channel.setInputStream(new ByteArrayInputStream((password + "\n").getBytes()));

            channel.connect();

            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Sudo command execution was interrupted.", e);
                    return "Sudo command execution interrupted.";
                }
            }
            
            String error = new String(errorBuffer.toByteArray());
            String output = new String(outputBuffer.toByteArray());

            // sudo itself might print to stderr on success (e.g., a warning), so we check the output first.
            if (!output.trim().isEmpty()) {
                log.debug("Sudo command output: {}", output);
                return output;
            }

            if (!error.isEmpty()) {
                log.error("Sudo command execution failed with error: {}", error);
                // The error might contain "try again", so we don't return it directly as success
                return ""; // Return empty to indicate failure
            }

            return output;

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            log.info("Disconnecting SSH session from {}", session.getHost());
            session.disconnect();
        }
        session = null;
    }
}
