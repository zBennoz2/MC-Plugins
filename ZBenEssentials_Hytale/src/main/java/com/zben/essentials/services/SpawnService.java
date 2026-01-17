package com.zben.essentials.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zben.essentials.model.PlayerLocation;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class SpawnService {
    private static final String SPAWN_FILE_NAME = "spawn.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDirectory;
    private PlayerLocation spawn;

    public SpawnService(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    public void loadOrCreate() {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create config directory", exception);
        }

        Path spawnPath = configDirectory.resolve(SPAWN_FILE_NAME);
        if (Files.notExists(spawnPath)) {
            spawn = null;
            return;
        }

        try (Reader reader = Files.newBufferedReader(spawnPath)) {
            spawn = gson.fromJson(reader, PlayerLocation.class);
        } catch (IOException exception) {
            spawn = null;
        }
    }

    public void save() {
        Path spawnPath = configDirectory.resolve(SPAWN_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(spawnPath)) {
            gson.toJson(spawn, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save spawn", exception);
        }
    }

    public Optional<PlayerLocation> getSpawn() {
        return Optional.ofNullable(spawn);
    }

    public void setSpawn(PlayerLocation location) {
        this.spawn = location;
        save();
    }
}
