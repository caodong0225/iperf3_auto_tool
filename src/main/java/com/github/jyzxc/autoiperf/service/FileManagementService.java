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
        } else if (filename.startsWith("test_")) {
            fileInfo.setType("client");
        } else {
            fileInfo.setType("client"); // Default to client
        }
        
        // Extract timestamp from filename if available
        if (filename.startsWith("test_")) {
            // Pattern: test_2026-01-09T00-03-37-6869426_f0e96ce7.json or test_20260109_001245_aa116982.json
            try {
                String timestampPart = filename.substring(5); // Remove "test_"
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

        // Only read first 2KB of file to extract metadata (to avoid loading large files)
        // Use head command to read only the beginning
        String headCommand = "head -c 2048 " + filePath + " 2>/dev/null";
        String jsonContent = null;
        try {
            jsonContent = sshService.executeCommand(headCommand, 3000);
        } catch (Exception e) {
            log.debug("Failed to read file head for {}: {}", filename, e.getMessage());
            // If file is empty or can't be read, still return file info
            return fileInfo;
        }

        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            // Empty file, still return info
            return fileInfo;
        }

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
     * Read JSON file content from remote machine.
     */
    public String readFileContent(SshService sshService, String filePath) throws Exception {
        log.debug("Reading file content: {}", filePath);
        String command = "cat " + filePath;
        return sshService.executeCommand(command, 5000);
    }
}

