package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.ClientTestConfig;
import com.github.jyzxc.autoiperf.model.ClientTestResult;
import com.github.jyzxc.autoiperf.model.IperfResult;
import com.github.jyzxc.autoiperf.sshtool.SshService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Handles the business logic for client-side iperf3 testing.
 */
public class ClientTestService {

    private static final Logger log = LoggerFactory.getLogger(ClientTestService.class);
    private static final String TEST_RESULTS_DIR = "test_results";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Execute a client test based on the provided configuration.
     * @param sshService SSH service connected to the client machine
     * @param config Test configuration
     * @return ClientTestResult containing the test results
     * @throws Exception if the test fails
     */
    public ClientTestResult executeTest(SshService sshService, ClientTestConfig config) throws Exception {
        log.info("Starting client test: target={}:{}, duration={}s, protocol={}", 
                config.getTargetHost(), config.getTargetPort(), config.getDuration(), config.getProtocol());

        // Build iperf3 client command
        String command = buildIperf3ClientCommand(config);
        log.debug("Executing iperf3 client command: {}", command);

        String testId = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try {
            // Execute the command with timeout (duration + 10 seconds buffer)
            int timeout = (config.getDuration() + 10) * 1000;
            String output = sshService.executeCommand(command, timeout);

            if (output == null || output.trim().isEmpty()) {
                throw new Exception("iperf3 command returned empty output");
            }

            log.debug("iperf3 client output: {}", output);

            // Parse JSON result
            IperfResult iperfResult = gson.fromJson(output, IperfResult.class);
            
            if (iperfResult == null) {
                throw new Exception("Failed to parse iperf3 JSON output");
            }

            // Check for errors in the result
            boolean success = (iperfResult.getError() == null || iperfResult.getError().isEmpty());
            String errorMessage = iperfResult.getError();

            // Build result object
            ClientTestResult result = ClientTestResult.builder()
                    .testId(testId)
                    .testTimestamp(timestamp)
                    .configuration(config)
                    .clientJsonResult(iperfResult)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();

            // Save to file
            saveTestResult(result);

            log.info("Client test completed: testId={}, success={}", testId, success);
            return result;

        } catch (Exception e) {
            log.error("Client test failed: {}", e.getMessage(), e);
            
            // Create error result
            ClientTestResult errorResult = ClientTestResult.builder()
                    .testId(testId)
                    .testTimestamp(timestamp)
                    .configuration(config)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();

            // Save error result
            saveTestResult(errorResult);
            
            throw e;
        }
    }

    /**
     * Build the iperf3 client command string based on configuration.
     */
    private String buildIperf3ClientCommand(ClientTestConfig config) {
        StringBuilder cmd = new StringBuilder("iperf3 -c ");
        cmd.append(config.getTargetHost());
        cmd.append(" -p ").append(config.getTargetPort());
        cmd.append(" -t ").append(config.getDuration());
        cmd.append(" -J"); // JSON output

        // Protocol
        if ("UDP".equalsIgnoreCase(config.getProtocol())) {
            cmd.append(" -u");
            if (config.getBandwidth() != null) {
                cmd.append(" -b ").append(config.getBandwidth());
            }
            if (config.getPacketSize() != null) {
                cmd.append(" -l ").append(config.getPacketSize());
            }
        } else {
            // TCP options
            if (config.getWindowSize() != null) {
                cmd.append(" -w ").append(config.getWindowSize());
            }
            if (config.getBandwidth() != null) {
                cmd.append(" -b ").append(config.getBandwidth());
            }
        }

        // Parallel streams
        if (config.getParallelStreams() != null && config.getParallelStreams() > 1) {
            cmd.append(" -P ").append(config.getParallelStreams());
        }

        // Reverse test
        if (config.getReverse() != null && config.getReverse()) {
            cmd.append(" -R");
        }

        // Bind address
        if (config.getSourceBindAddress() != null && !config.getSourceBindAddress().isEmpty()) {
            cmd.append(" -B ").append(config.getSourceBindAddress());
        }

        return cmd.toString();
    }

    /**
     * Save test result to JSON file in test_results directory.
     */
    private void saveTestResult(ClientTestResult result) throws IOException {
        // Ensure directory exists
        File resultsDir = new File(TEST_RESULTS_DIR);
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
            log.info("Created test_results directory");
        }

        // Generate filename: timestamp_targetIP.json
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String targetInfo = result.getConfiguration().getTargetHost().replace(".", "_");
        String filename = String.format("%s_%s.json", timestamp, targetInfo);
        File resultFile = new File(resultsDir, filename);

        // Write JSON
        try (FileWriter writer = new FileWriter(resultFile)) {
            gson.toJson(result, writer);
            log.info("Saved test result to: {}", resultFile.getAbsolutePath());
        }
    }
}

