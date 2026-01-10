package com.github.jyzxc.autoiperf.sshtool;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

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
        return executeCommand(command, 30000); // Default 30 second timeout
    }

    public String executeCommand(String command, int timeoutMs) throws JSchException {
        if (session == null || !session.isConnected()) {
            log.error("Cannot execute command. Session is not connected.");
            throw new JSchException("Session is not connected.");
        }

        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
            channel.setOutputStream(outputBuffer);
            channel.setErrStream(errorBuffer);

            log.debug("Executing command with {}ms timeout: {}", timeoutMs, command);
            channel.connect();

            long startTime = System.currentTimeMillis();
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed > timeoutMs) {
                        log.error("Command execution timed out after {}ms. Command: {}", elapsed, command);
                        channel.disconnect();
                        throw new JSchException("Command execution timed out after " + elapsed + "ms");
                    }
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

    /**
     * Download a remote file to local path via SFTP.
     * @param remotePath remote absolute path (e.g. /tmp/iperf3/test_xxx.json)
     * @param localPath local file path
     */
    public void downloadFile(String remotePath, String localPath) throws JSchException, SftpException {
        if (session == null || !session.isConnected()) {
            log.error("Cannot download file. Session is not connected.");
            throw new JSchException("Session is not connected.");
        }
        if (remotePath == null || remotePath.isBlank()) {
            throw new IllegalArgumentException("remotePath is blank");
        }
        if (localPath == null || localPath.isBlank()) {
            throw new IllegalArgumentException("localPath is blank");
        }

        File localFile = new File(localPath);
        File parent = localFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create local directory: " + parent.getAbsolutePath());
        }

        ChannelSftp sftp = null;
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect(10_000);
            sftp = (ChannelSftp) channel;
            log.info("Downloading remote file '{}' to '{}'", remotePath, localFile.getAbsolutePath());
            sftp.get(remotePath, localFile.getAbsolutePath());
        } finally {
            if (sftp != null) {
                try {
                    sftp.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
