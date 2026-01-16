package com.zben.essentials.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.zben.essentials.config.Config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigService {
    private static final String CONFIG_FILE_NAME = "config.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDirectory;
    private Config config;

    public ConfigService(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    public void loadOrCreate() {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create config directory", exception);
        }

        Path configPath = configDirectory.resolve(CONFIG_FILE_NAME);
        if (Files.notExists(configPath)) {
            config = new Config();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            config = gson.fromJson(reader, Config.class);
            if (config == null) {
                config = new Config();
            }
        } catch (IOException | JsonSyntaxException exception) {
            config = new Config();
        }
    }

    public void save() {
        Path configPath = configDirectory.resolve(CONFIG_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(config, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save config", exception);
        }
    }

    public Config getConfig() {
        return config;
    }
}
