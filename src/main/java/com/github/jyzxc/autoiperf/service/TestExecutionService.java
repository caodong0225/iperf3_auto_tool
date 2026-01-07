public class TestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionService.class);
    private final Gson gson = new Gson();

    public IperfResult executeClient(SshService clientSsh, TestConfig config) throws Exception {
        log.info("Executing client test...");
        String command = String.format("iperf3 -c %s -p %d -B %s -t %d -l %s -J",
                config.getServerBindAddress(),
                config.getTestPort(),
                config.getClientBindAddress(),
                config.getDuration(),
                config.getPacketSize()
        );

        log.debug("Client command: {}", command);
        String clientJsonOutput = clientSsh.executeCommand(command);
        log.debug("Client raw JSON output: {}", clientJsonOutput);

        if (clientJsonOutput.contains("error")) {
             throw new Exception("iperf3 client test failed: " + clientJsonOutput);
        }

        return gson.fromJson(clientJsonOutput, IperfResult.class);
    }

    public String startServer(SshService serverSsh, TestConfig config) throws Exception {
        log.info("Starting iperf3 server in the background...");
        String tempFileCommand = "mktemp -p /tmp iperf-server.XXXXXX.json";
        String tempFilePath = serverSsh.executeCommand(tempFileCommand).trim();

        if (tempFilePath.isEmpty() || tempFilePath.contains("Error")) {
            log.error("Failed to create temp file on server: {}", tempFilePath);
            throw new Exception("Failed to create temp file on server. Check /tmp directory permissions.");
        }
        log.info("Server temp file created at: {}", tempFilePath);

        String serverCommand = String.format("iperf3 -s -p %d -B %s -J --logfile %s & echo $!",
                config.getTestPort(),
                config.getServerBindAddress(),
                tempFilePath
        );
        log.debug("Server command: {}", serverCommand);
        String serverPid = serverSsh.executeCommand(serverCommand).trim();

        if (serverPid.isEmpty() || !serverPid.matches("\\d+")) {
             log.error("Failed to get a valid PID for the iperf3 server process. Output: {}", serverPid);
             throw new Exception("Failed to start iperf3 server or get its PID.");
        }
        log.info("iPerf3 server started with PID: {} on host: {}. Now verifying it is listening.", serverPid, config.getServerHost());

        // ** Polling mechanism to ensure server is ready **
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds
        boolean isListening = false;
        String checkListenCommand = String.format("ss -tlpn | grep ':%d'", config.getTestPort());
        
        while (System.currentTimeMillis() - startTime < timeout) {
            String checkResult = serverSsh.executeCommand(checkListenCommand);
            if (checkResult != null && !checkResult.trim().isEmpty() && checkResult.contains(String.valueOf(config.getTestPort()))) {
                log.info("Server is listening on port {}. Proceeding with test.", config.getTestPort());
                isListening = true;
                break;
            }
            Thread.sleep(200); // Poll every 200ms
        }

        if (!isListening) {
            log.error("Server did not start listening on port {} within {}ms.", config.getTestPort(), timeout);
            cleanupServer(serverSsh, tempFilePath, serverPid); // Attempt to clean up the failed process
            throw new Exception("Server failed to start listening on port " + config.getTestPort() + " within the timeout period.");
        }

        return tempFilePath + ";" + serverPid;
    }

    public IperfResult collectServerResult(SshService serverSsh, String tempFilePath) throws Exception {
        log.info("Collecting server results from {}", tempFilePath);
        String serverJsonOutput = serverSsh.executeCommand("cat " + tempFilePath);
        log.debug("Server raw JSON output: {}", serverJsonOutput);
        return gson.fromJson(serverJsonOutput, IperfResult.class);
    }

    public void cleanupServer(SshService serverSsh, String tempFilePath, String pid) {
        log.info("Cleaning up server resources (PID: {}, File: {})", pid, tempFilePath);
        try {
            if (pid != null && !pid.isEmpty()) {
                serverSsh.executeCommand("kill " + pid);
                log.info("Killed server process with PID: {}", pid);
            }
            if (tempFilePath != null && !tempFilePath.isEmpty()) {
                serverSsh.executeCommand("rm " + tempFilePath);
                log.info("Removed server temp file: {}", tempFilePath);
            }
        } catch (Exception e) {
            log.error("Error during server cleanup. Manual cleanup may be required.", e);
        }
    }
}
