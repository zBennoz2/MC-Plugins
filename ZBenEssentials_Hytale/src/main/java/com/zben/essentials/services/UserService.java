package com.zben.essentials.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserService {
    private static final String USERS_FILE_NAME = "users.json";
    private static final Type USERS_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDirectory;
    private Map<String, String> users = new HashMap<>();

    public UserService(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    public void loadOrCreate() {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create config directory", exception);
        }

        Path usersPath = configDirectory.resolve(USERS_FILE_NAME);
        if (Files.notExists(usersPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(usersPath)) {
            Map<String, String> loaded = gson.fromJson(reader, USERS_TYPE);
            if (loaded != null) {
                users = loaded;
            }
        } catch (IOException exception) {
            users = new HashMap<>();
        }
    }

    public void save() {
        Path usersPath = configDirectory.resolve(USERS_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(usersPath)) {
            gson.toJson(users, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save users", exception);
        }
    }

    public String getGroup(UUID playerId) {
        return users.get(playerId.toString());
    }

    public void setGroup(UUID playerId, String group) {
        users.put(playerId.toString(), group);
        save();
    }
}
