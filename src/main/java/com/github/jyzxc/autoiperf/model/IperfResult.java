package com.github.jyzxc.autoiperf.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Represents the full JSON output from an iperf3 test.
 * This class is designed to be deserialized by Gson.
 */
@Data
public class IperfResult {

    private Start start;
    private End end;

    @Data
    public static class End {
        @SerializedName("sum_sent")
        private SumSection sumSent;

        @SerializedName("sum_received")
        private SumSection sumReceived;

        @SerializedName("cpu_utilization_percent")
        private CpuUtilization cpuUtilizationPercent;
    }

    @Data
    public static class SumSection {
        private long bytes;
        @SerializedName("bits_per_second")
        private double bitsPerSecond;
    }

    @Data
    public static class CpuUtilization {
        @SerializedName("host_total")
        private double hostTotal;
        @SerializedName("remote_total")
        private double remoteTotal;
    }
    
    // Minimal start section for now
    @Data
    public static class Start {
        private List<Connected> connected;
    }
    
    @Data
    public static class Connected {
        @SerializedName("remote_host")
        private String remoteHost;
        @SerializedName("remote_port")
        private int remotePort;
    }
}
