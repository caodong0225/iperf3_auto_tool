package com.github.jyzxc.autoiperf.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the result of a client test execution.
 */
@Data
@Builder
public class ClientTestResult {
    private String testId;                  // UUID
    private String testTimestamp;           // ISO timestamp
    private ClientTestConfig configuration; // Test configuration
    private IperfResult clientJsonResult;   // Only contains client-side iperf3 -J output
    private boolean success;                // Whether the test succeeded
    private String errorMessage;            // Error message if test failed
}

