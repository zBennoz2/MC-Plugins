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
import java.util.Map;

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

        boolean updated = applyDefaults(config, new Config());
        if (updated) {
            save();
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

    private boolean applyDefaults(Config target, Config defaults) {
        boolean changed = false;
        if (target.getLanguage() == null) {
            target.setLanguage(defaults.getLanguage());
            changed = true;
        }
        if (target.getPrefix() == null) {
            target.setPrefix(defaults.getPrefix());
            changed = true;
        }
        if (target.getWelcome() == null) {
            target.setWelcome(defaults.getWelcome());
            changed = true;
        }
        if (target.getJoinQuit() == null) {
            target.setJoinQuit(defaults.getJoinQuit());
            changed = true;
        }
        if (target.getChatFormats() == null) {
            target.setChatFormats(defaults.getChatFormats());
            changed = true;
        } else {
            for (Map.Entry<String, Config.ChatFormatConfig> entry : defaults.getChatFormats().entrySet()) {
                if (!target.getChatFormats().containsKey(entry.getKey())) {
                    target.getChatFormats().put(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        }
        if (target.getGroups() == null) {
            target.setGroups(defaults.getGroups());
            changed = true;
        } else {
            for (Map.Entry<String, Config.GroupConfig> entry : defaults.getGroups().entrySet()) {
                if (!target.getGroups().containsKey(entry.getKey())) {
                    target.getGroups().put(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        }
        if (target.getDefaultGroup() == null) {
            target.setDefaultGroup(defaults.getDefaultGroup());
            changed = true;
        }
        if (target.getHomeLimits() == null) {
            target.setHomeLimits(defaults.getHomeLimits());
            changed = true;
        } else {
            for (Map.Entry<String, Integer> entry : defaults.getHomeLimits().entrySet()) {
                if (!target.getHomeLimits().containsKey(entry.getKey())) {
                    target.getHomeLimits().put(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        }
        if (target.getTpa() == null) {
            target.setTpa(defaults.getTpa());
            changed = true;
        }
        if (target.getBack() == null) {
            target.setBack(defaults.getBack());
            changed = true;
        }
        if (target.getSpawn() == null) {
            target.setSpawn(defaults.getSpawn());
            changed = true;
        }
        if (target.getWarps() == null) {
            target.setWarps(defaults.getWarps());
            changed = true;
        }
        if (target.getMessageOverrides() == null) {
            target.setMessageOverrides(defaults.getMessageOverrides());
            changed = true;
        }
        return changed;
    }
}
