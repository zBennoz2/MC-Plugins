package com.zben.essentials.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zben.essentials.model.Warp;

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

public class WarpService {
    private static final String WARPS_FILE_NAME = "warps.json";
    private static final Type WARPS_TYPE = new TypeToken<Map<String, Warp>>() {}.getType();
    private static final int MAX_NAME_LENGTH = 24;
    private static final String NAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDirectory;
    private Map<String, Warp> warps = new HashMap<>();

    public WarpService(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    public void loadOrCreate() {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create config directory", exception);
        }

        Path warpsPath = configDirectory.resolve(WARPS_FILE_NAME);
        if (Files.notExists(warpsPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(warpsPath)) {
            Map<String, Warp> loaded = gson.fromJson(reader, WARPS_TYPE);
            if (loaded != null) {
                warps = loaded;
            }
        } catch (IOException exception) {
            warps = new HashMap<>();
        }
    }

    public void save() {
        Path warpsPath = configDirectory.resolve(WARPS_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(warpsPath)) {
            gson.toJson(warps, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save warps", exception);
        }
    }

    public boolean isValidWarpName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return !trimmed.isEmpty()
                && trimmed.length() <= MAX_NAME_LENGTH
                && trimmed.matches(NAME_PATTERN);
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(normalizeName(name));
    }

    public Warp getWarp(String name) {
        return warps.get(normalizeName(name));
    }

    public void setWarp(String name, Warp warp) {
        warps.put(normalizeName(name), warp);
        save();
    }

    public boolean removeWarp(String name) {
        Warp removed = warps.remove(normalizeName(name));
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public List<String> listWarps() {
        return new ArrayList<>(warps.keySet());
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
