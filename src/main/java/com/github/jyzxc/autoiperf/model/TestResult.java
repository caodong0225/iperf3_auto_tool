package com.github.jyzxc.autoiperf.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestResult {
    private String testId;
    private String testTimestamp;
    private TestConfig configuration;
    private IperfResult clientResult;
    private IperfResult serverResult;
    private boolean success;
    private String errorMessage;
}
