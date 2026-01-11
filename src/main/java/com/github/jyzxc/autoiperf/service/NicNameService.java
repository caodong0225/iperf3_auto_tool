package com.github.jyzxc.autoiperf.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to manage custom names for network interface IP addresses.
 * Stores mappings in nic_names.json file.
 */
public class NicNameService {

    private static final Logger log = LoggerFactory.getLogger(NicNameService.class);
    private static final Path NIC_NAMES_PATH = Paths.get("nic_names.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, String> nicNames = new HashMap<>(); // IP -> Custom Name

    public NicNameService() {
        loadNicNames();
    }

    /**
     * Load NIC name mappings from file.
     */
    private void loadNicNames() {
        if (!Files.exists(NIC_NAMES_PATH)) {
            nicNames = new HashMap<>();
            return;
        }
        try (FileReader reader = new FileReader(NIC_NAMES_PATH.toFile())) {
            @SuppressWarnings("unchecked")
            Map<String, String> loaded = gson.fromJson(reader, Map.class);
            nicNames = loaded != null ? loaded : new HashMap<>();
            log.info("Loaded {} NIC name mappings from {}", nicNames.size(), NIC_NAMES_PATH);
        } catch (IOException e) {
            log.error("Failed to load NIC names from {}", NIC_NAMES_PATH, e);
            nicNames = new HashMap<>();
        }
    }

    /**
     * Save NIC name mappings to file.
     */
    private void saveNicNames() {
        try (FileWriter writer = new FileWriter(NIC_NAMES_PATH.toFile())) {
            gson.toJson(nicNames, writer);
            log.info("Saved {} NIC name mappings to {}", nicNames.size(), NIC_NAMES_PATH);
        } catch (IOException e) {
            log.error("Failed to save NIC names to {}", NIC_NAMES_PATH, e);
        }
    }

    /**
     * Get custom name for an IP address.
     * @param ip The IP address
     * @return The custom name, or null if not set
     */
    public String getNicName(String ip) {
        return nicNames.get(ip);
    }

    /**
     * Set custom name for an IP address.
     * @param ip The IP address
     * @param name The custom name (can be null to remove)
     */
    public void setNicName(String ip, String name) {
        if (name == null || name.trim().isEmpty()) {
            nicNames.remove(ip);
        } else {
            nicNames.put(ip, name.trim());
        }
        saveNicNames();
    }

    /**
     * Format IP address for display with custom name if available.
     * @param ip The IP address
     * @return Formatted string: "IP (Name)" or just "IP"
     */
    public String formatIpWithName(String ip) {
        String name = getNicName(ip);
        if (name != null && !name.trim().isEmpty()) {
            return String.format("%s (%s)", ip, name);
        }
        return ip;
    }

    /**
     * Extract IP address from formatted display string.
     * @param displayText The display text (e.g., "192.168.1.1 (eth0)" or "192.168.1.1")
     * @return The IP address
     */
    public String extractIp(String displayText) {
        if (displayText == null || displayText.trim().isEmpty()) {
            return null;
        }
        // If it contains " (", extract the part before it
        int parenIndex = displayText.indexOf(" (");
        if (parenIndex > 0) {
            return displayText.substring(0, parenIndex).trim();
        }
        return displayText.trim();
    }
}
