package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.sshtool.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing iperf3 JSON result files on remote machines.
 */
public class FileManagementService {

    private static final Logger log = LoggerFactory.getLogger(FileManagementService.class);

    /**
     * Represents a JSON file metadata.
     */
    public static class JsonFileInfo {
        private String filename;
        private String filePath;
        private String host;
        private String type; // "server" or "client"
        private String serverIp;
        private int port;
        private Integer pid;
        private String clientIp; // Only for client files
        private String timestamp;
        private long fileSize;
        private Double sendRate; // bits_per_second from sum_sent
        private Double receiveRate; // bits_per_second from sum_received
        private Double sendCpu; // host_total from cpu_utilization_percent
        private Double receiveCpu; // remote_total from cpu_utilization_percent

        // Getters and setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getServerIp() { return serverIp; }
        public void setServerIp(String serverIp) { this.serverIp = serverIp; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public Integer getPid() { return pid; }
        public void setPid(Integer pid) { this.pid = pid; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public Double getSendRate() { return sendRate; }
        public void setSendRate(Double sendRate) { this.sendRate = sendRate; }
        public Double getReceiveRate() { return receiveRate; }
        public void setReceiveRate(Double receiveRate) { this.receiveRate = receiveRate; }
        public Double getSendCpu() { return sendCpu; }
        public void setSendCpu(Double sendCpu) { this.sendCpu = sendCpu; }
        public Double getReceiveCpu() { return receiveCpu; }
        public void setReceiveCpu(Double receiveCpu) { this.receiveCpu = receiveCpu; }
    }

    /**
     * List all JSON files in /tmp/iperf3 directory on remote machine.
     */
    public List<JsonFileInfo> listJsonFiles(SshService sshService, String host) throws Exception {
        log.info("Listing JSON files in /tmp/iperf3 on host: {}", host);
        List<JsonFileInfo> files = new ArrayList<>();

        // List all files in /tmp/iperf3
        String listCommand = "ls -lh /tmp/iperf3/*.json 2>/dev/null";
        String output = sshService.executeCommand(listCommand, 5000);

        if (output == null || output.trim().isEmpty()) {
            log.info("No JSON files found in /tmp/iperf3 on host: {}", host);
            return files;
        }

        log.debug("File list output: {}", output);

        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("total")) continue;

            // Parse ls -lh output: -rw-r--r-- 1 root root 1234 Jan  9 00:12 /tmp/iperf3/test_xxx.json
            // Extract filename and size
            String[] parts = line.split("\\s+");
            if (parts.length < 5) continue;

            // Size is typically at index 4 (0-indexed), filename is the last part
            String fileSize = parts.length > 4 ? parts[4] : "0";
            String filePath = parts[parts.length - 1];
            
            // Skip if not a full path
            if (!filePath.startsWith("/")) {
                continue;
            }
            
            String filename = filePath.substring(filePath.lastIndexOf('/') + 1);
            
            // Skip if not a JSON file
            if (!filename.endsWith(".json")) {
                continue;
            }

            try {
                JsonFileInfo fileInfo = parseJsonFile(sshService, host, filePath, filename, fileSize);
                if (fileInfo != null) {
                    files.add(fileInfo);
                }
            } catch (Exception e) {
                log.warn("Failed to parse file {}: {}", filePath, e.getMessage());
            }
        }

        return files;
    }

    /**
     * Parse a JSON file and extract metadata.
     * Only reads the first 2KB of the file to avoid loading large files.
     */
    private JsonFileInfo parseJsonFile(SshService sshService, String host, String filePath, 
                                      String filename, String fileSizeStr) throws Exception {
        JsonFileInfo fileInfo = new JsonFileInfo();
        fileInfo.setFilename(filename);
        fileInfo.setFilePath(filePath);
        fileInfo.setHost(host);
        fileInfo.setFileSize(parseFileSize(fileSizeStr));

        // Determine file type from filename
        if (filename.startsWith("server-")) {
            fileInfo.setType("server");
        } else if (filename.startsWith("test_") || filename.startsWith("send_") || filename.startsWith("receive_")) {
            fileInfo.setType("client");
        } else {
            fileInfo.setType("client"); // Default to client
        }
        
        // Extract timestamp from filename if available
        if (filename.startsWith("test_") || filename.startsWith("send_") || filename.startsWith("receive_")) {
            // Pattern: test_20260109_001245_aa116982.json or send_20260109_001245_aa116982.json or receive_20260109_001245_aa116982.json
            try {
                String prefix = filename.startsWith("test_") ? "test_" : 
                               filename.startsWith("send_") ? "send_" : "receive_";
                String timestampPart = filename.substring(prefix.length()); // Remove prefix
                int underscoreIndex = timestampPart.indexOf('_');
                if (underscoreIndex > 0) {
                    timestampPart = timestampPart.substring(0, underscoreIndex);
                    // Try to format timestamp
                    if (timestampPart.contains("T")) {
                        // Format: 2026-01-09T00-03-37-6869426
                        timestampPart = timestampPart.replace("T", " ").replace("-", ":");
                        if (timestampPart.length() > 19) {
                            timestampPart = timestampPart.substring(0, 19);
                        }
                    } else if (timestampPart.length() >= 15) {
                        // Format: 20260109_001245
                        timestampPart = timestampPart.replace("_", " ");
                        if (timestampPart.length() >= 15) {
                            timestampPart = timestampPart.substring(0, 4) + "-" + 
                                          timestampPart.substring(4, 6) + "-" + 
                                          timestampPart.substring(6, 8) + " " + 
                                          timestampPart.substring(9, 11) + ":" + 
                                          timestampPart.substring(11, 13) + ":" + 
                                          timestampPart.substring(13, 15);
                        }
                    }
                    fileInfo.setTimestamp(timestampPart);
                }
            } catch (Exception e) {
                log.debug("Failed to extract timestamp from filename: {}", e.getMessage());
            }
        }

        // Read first 2KB for basic metadata (remote_host, remote_port, local_host)
        String headCommand = "head -c 2048 " + filePath + " 2>/dev/null";
        String jsonContent = null;
        try {
            jsonContent = sshService.executeCommand(headCommand, 3000);
        } catch (Exception e) {
            log.debug("Failed to read file head for {}: {}", filename, e.getMessage());
            // If file is empty or can't be read, still return file info
            return fileInfo;
        }

        if (jsonContent != null && !jsonContent.trim().isEmpty()) {
            // Parse JSON content (only first part) using regex for faster extraction
            try {
                // Extract key information using regex (faster than full JSON parse)
                // Extract remote_host and remote_port from JSON
                Pattern remoteHostPattern = Pattern.compile("\"remote_host\"\\s*:\\s*\"([^\"]+)\"");
                Pattern remotePortPattern = Pattern.compile("\"remote_port\"\\s*:\\s*(\\d+)");
                Pattern localHostPattern = Pattern.compile("\"local_host\"\\s*:\\s*\"([^\"]+)\"");
                
                Matcher remoteHostMatcher = remoteHostPattern.matcher(jsonContent);
                if (remoteHostMatcher.find()) {
                    fileInfo.setServerIp(remoteHostMatcher.group(1));
                }
                
                Matcher remotePortMatcher = remotePortPattern.matcher(jsonContent);
                if (remotePortMatcher.find()) {
                    try {
                        fileInfo.setPort(Integer.parseInt(remotePortMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
                
                // For client files, extract local_host as client IP
                if ("client".equals(fileInfo.getType())) {
                    Matcher localHostMatcher = localHostPattern.matcher(jsonContent);
                    if (localHostMatcher.find()) {
                        fileInfo.setClientIp(localHostMatcher.group(1));
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse JSON content for file {}: {}", filename, e.getMessage());
            }
        }

        // Read last 55 lines of file to extract performance metrics (sum_sent, sum_received, cpu_utilization_percent)
        String tailCommand = "tail -n 55 " + filePath + " 2>/dev/null";
        String tailContent = null;
        try {
            tailContent = sshService.executeCommand(tailCommand, 3000);
        } catch (Exception e) {
            log.debug("Failed to read file tail for {}: {}", filename, e.getMessage());
            // Continue without tail content
        }

        if (tailContent != null && !tailContent.trim().isEmpty()) {
            try {
                // Use DOTALL mode to match across lines (JSON may be formatted with newlines)
                // Extract sum_sent.bits_per_second
                // Pattern matches: "sum_sent": { ... "bits_per_second": 123.456 ... }
                Pattern sumSentPattern = Pattern.compile("\"sum_sent\"\\s*:\\s*\\{[\\s\\S]*?\"bits_per_second\"\\s*:\\s*([0-9.]+)", Pattern.DOTALL);
                Matcher sumSentMatcher = sumSentPattern.matcher(tailContent);
                if (sumSentMatcher.find()) {
                    try {
                        fileInfo.setSendRate(Double.parseDouble(sumSentMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        log.debug("Failed to parse sendRate: {}", e.getMessage());
                    }
                }

                // Extract sum_received.bits_per_second
                Pattern sumReceivedPattern = Pattern.compile("\"sum_received\"\\s*:\\s*\\{[\\s\\S]*?\"bits_per_second\"\\s*:\\s*([0-9.]+)", Pattern.DOTALL);
                Matcher sumReceivedMatcher = sumReceivedPattern.matcher(tailContent);
                if (sumReceivedMatcher.find()) {
                    try {
                        fileInfo.setReceiveRate(Double.parseDouble(sumReceivedMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        log.debug("Failed to parse receiveRate: {}", e.getMessage());
                    }
                }

                // Extract cpu_utilization_percent.host_total
                // Pattern matches: "cpu_utilization_percent": { ... "host_total": 72.03 ... }
                Pattern hostTotalPattern = Pattern.compile("\"cpu_utilization_percent\"\\s*:\\s*\\{[\\s\\S]*?\"host_total\"\\s*:\\s*([0-9.]+)", Pattern.DOTALL);
                Matcher hostTotalMatcher = hostTotalPattern.matcher(tailContent);
                if (hostTotalMatcher.find()) {
                    try {
                        fileInfo.setSendCpu(Double.parseDouble(hostTotalMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        log.debug("Failed to parse sendCpu: {}", e.getMessage());
                    }
                }

                // Extract cpu_utilization_percent.remote_total
                Pattern remoteTotalPattern = Pattern.compile("\"cpu_utilization_percent\"\\s*:\\s*\\{[\\s\\S]*?\"remote_total\"\\s*:\\s*([0-9.]+)", Pattern.DOTALL);
                Matcher remoteTotalMatcher = remoteTotalPattern.matcher(tailContent);
                if (remoteTotalMatcher.find()) {
                    try {
                        fileInfo.setReceiveCpu(Double.parseDouble(remoteTotalMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        log.debug("Failed to parse receiveCpu: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse tail JSON content for file {}: {}", filename, e.getMessage());
            }
        }

        // Try to extract PID from filename or process
        extractPidFromFilename(fileInfo, filename);

        return fileInfo;
    }

    /**
     * Extract PID from filename pattern or running process.
     */
    private void extractPidFromFilename(JsonFileInfo fileInfo, String filename) {
        // Try to find PID in filename (e.g., server-xxx-pid.json)
        Pattern pidPattern = Pattern.compile("-(\\d+)\\.json$");
        Matcher matcher = pidPattern.matcher(filename);
        if (matcher.find()) {
            try {
                fileInfo.setPid(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }


    /**
     * Parse file size string (e.g., "1.2K", "3.4M") to bytes.
     */
    private long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 0;
        }
        try {
            sizeStr = sizeStr.trim().toUpperCase();
            if (sizeStr.endsWith("K")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024);
            } else if (sizeStr.endsWith("M")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024 * 1024);
            } else if (sizeStr.endsWith("G")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024 * 1024 * 1024);
            } else {
                return Long.parseLong(sizeStr);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Delete a JSON file from remote machine.
     */
    public void deleteFile(SshService sshService, String filePath) throws Exception {
        log.info("Deleting file: {}", filePath);
        String command = "rm -f " + filePath;
        sshService.executeCommand(command, 3000);
    }

    /**
     * Download a file from remote machine to local path.
     */
    public void downloadFile(SshService sshService, String remotePath, String localPath) throws Exception {
        log.info("Downloading file from '{}' to '{}'", remotePath, localPath);
        sshService.downloadFile(remotePath, localPath);
    }

    /**
     * Read JSON file content from remote machine.
     */
    public String readFileContent(SshService sshService, String filePath) throws Exception {
        log.debug("Reading file content: {}", filePath);
        String command = "cat " + filePath;
        return sshService.executeCommand(command, 5000);
    }
}

