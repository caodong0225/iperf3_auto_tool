package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.TestResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ResultPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ResultPersistenceService.class);
    private static final Path RESULTS_DIR = Paths.get("test_results");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ResultPersistenceService() {
        try {
            Files.createDirectories(RESULTS_DIR);
        } catch (IOException e) {
            log.error("Failed to create results directory: {}", RESULTS_DIR, e);
        }
    }

    public void saveResult(TestResult result) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s.json", timestamp, result.getConfiguration().getServerHost());
        Path filePath = RESULTS_DIR.resolve(fileName);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(result, writer);
            log.info("Test result saved to: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save test result to file: {}", filePath, e);
        }
    }
}
