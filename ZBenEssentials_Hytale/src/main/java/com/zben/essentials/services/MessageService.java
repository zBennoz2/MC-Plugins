package com.zben.essentials.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zben.essentials.config.Config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageService {
    private static final Type LANG_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private final Gson gson = new Gson();
    private final ConfigService configService;
    private Map<String, String> messages = new HashMap<>();

    public MessageService(ConfigService configService) {
        this.configService = configService;
    }

    public void loadLanguage() {
        Config config = configService.getConfig();
        String language = config.getLanguage();
        String resourcePath = String.format("/lang/%s.json", language);
        try (InputStreamReader reader = new InputStreamReader(
                MessageService.class.getResourceAsStream(resourcePath),
                StandardCharsets.UTF_8
        )) {
            Map<String, String> loaded = gson.fromJson(reader, LANG_TYPE);
            if (loaded != null) {
                messages = loaded;
            }
        } catch (IOException | NullPointerException exception) {
            messages = new HashMap<>();
        }
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        Config config = configService.getConfig();
        String template = resolveOverride(config, key);
        if (template == null) {
            template = messages.getOrDefault(key, key);
        }
        String resolved = template;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return resolved;
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    private String resolveOverride(Config config, String key) {
        if (config.getMessageOverrides().containsKey(key)) {
            return config.getMessageOverrides().get(key);
        }
        if (\"join.message\".equals(key)) {
            return config.getJoinQuit().getJoinMessage();
        }
        if (\"quit.message\".equals(key)) {
            return config.getJoinQuit().getQuitMessage();
        }
        return null;
    }
}
