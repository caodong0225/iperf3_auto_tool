package com.github.jyzxc.autoiperf.sshtool;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;

public class SshService {

    private Session session;

    public void connect(String username, String password, String host, int port) throws JSchException {
        if (session != null && session.isConnected()) {
            disconnect();
        }
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no"); // For simplicity, auto-accept host key
        session.connect(10000); // 10-second timeout
    }

    public String executeCommand(String command) throws JSchException {
        if (session == null || !session.isConnected()) {
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

            channel.connect();

            // Wait for the command to complete
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Command execution interrupted.";
                }
            }

            String error = new String(errorBuffer.toByteArray());
            if (!error.isEmpty()) {
                return "Error: " + error;
            }

            return new String(outputBuffer.toByteArray());

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        session = null;
    }
}
