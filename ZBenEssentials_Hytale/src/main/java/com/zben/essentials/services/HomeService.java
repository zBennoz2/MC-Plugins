package com.zben.essentials.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zben.essentials.model.Home;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HomeService {
    private static final String HOMES_FILE_NAME = "homes.json";
    private static final Type HOMES_TYPE = new TypeToken<Map<String, Map<String, Home>>>() {}.getType();
    private static final int MAX_NAME_LENGTH = 24;
    private static final String NAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDirectory;
    private Map<String, Map<String, Home>> homesByPlayer = new HashMap<>();

    public HomeService(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    public void loadOrCreate() {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create config directory", exception);
        }

        Path homesPath = configDirectory.resolve(HOMES_FILE_NAME);
        if (Files.notExists(homesPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(homesPath)) {
            Map<String, Map<String, Home>> loaded = gson.fromJson(reader, HOMES_TYPE);
            if (loaded != null) {
                homesByPlayer = loaded;
            }
        } catch (IOException exception) {
            homesByPlayer = new HashMap<>();
        }
    }

    public void save() {
        Path homesPath = configDirectory.resolve(HOMES_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(homesPath)) {
            gson.toJson(homesByPlayer, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save homes", exception);
        }
    }

    public boolean isValidHomeName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return !trimmed.isEmpty()
                && trimmed.length() <= MAX_NAME_LENGTH
                && trimmed.matches(NAME_PATTERN);
    }

    public boolean hasHome(UUID playerId, String name) {
        return getHomes(playerId).containsKey(normalizeName(name));
    }

    public Home getHome(UUID playerId, String name) {
        return getHomes(playerId).get(normalizeName(name));
    }

    public void setHome(UUID playerId, String name, Home home) {
        Map<String, Home> homes = getHomes(playerId);
        homes.put(normalizeName(name), home);
        save();
    }

    public boolean removeHome(UUID playerId, String name) {
        Map<String, Home> homes = getHomes(playerId);
        Home removed = homes.remove(normalizeName(name));
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public List<String> listHomes(UUID playerId) {
        Map<String, Home> homes = getHomes(playerId);
        return new ArrayList<>(homes.keySet());
    }

    public int countHomes(UUID playerId) {
        return getHomes(playerId).size();
    }

    private Map<String, Home> getHomes(UUID playerId) {
        return homesByPlayer.computeIfAbsent(playerId.toString(), key -> new HashMap<>());
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
