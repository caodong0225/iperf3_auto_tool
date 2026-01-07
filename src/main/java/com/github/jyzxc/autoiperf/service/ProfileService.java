package com.github.jyzxc.autoiperf.service;

import com.github.jyzxc.autoiperf.model.SshProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private static final Path PROFILE_PATH = Paths.get("profiles.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public List<SshProfile> loadProfiles() {
        if (!Files.exists(PROFILE_PATH)) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(PROFILE_PATH.toFile())) {
            Type listType = new TypeToken<ArrayList<SshProfile>>() {}.getType();
            List<SshProfile> profiles = gson.fromJson(reader, listType);
            log.info("Loaded {} profiles from {}", profiles.size(), PROFILE_PATH);
            return profiles != null ? profiles : new ArrayList<>();
        } catch (IOException e) {
            log.error("Failed to load profiles from {}", PROFILE_PATH, e);
            return new ArrayList<>();
        }
    }

    public void saveProfiles(List<SshProfile> profiles) {
        try (FileWriter writer = new FileWriter(PROFILE_PATH.toFile())) {
            gson.toJson(profiles, writer);
            log.info("Saved {} profiles to {}", profiles.size(), PROFILE_PATH);
        } catch (IOException e) {
            log.error("Failed to save profiles to {}", PROFILE_PATH, e);
        }
    }

    public void saveProfile(SshProfile profile) {
        List<SshProfile> profiles = loadProfiles();
        // Remove existing profile with the same name to avoid duplicates
        profiles.removeIf(p -> p.getProfileName().equals(profile.getProfileName()));
        profiles.add(profile);
        saveProfiles(profiles);
    }

    public void deleteProfile(String profileName) {
        List<SshProfile> profiles = loadProfiles();
        boolean removed = profiles.removeIf(p -> p.getProfileName().equals(profileName));
        if (removed) {
            saveProfiles(profiles);
        }
    }
}
