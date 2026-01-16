package com.zben.essentials;

import com.zben.essentials.commands.ZBenCommand;
import com.zben.essentials.services.ConfigService;
import com.zben.essentials.services.MessageService;
import com.zben.essentials.services.PermissionService;
import com.zben.essentials.services.UserService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ZBenEssentialsMod {
    private ConfigService configService;
    private MessageService messageService;
    private UserService userService;
    private PermissionService permissionService;
    private ZBenCommand zBenCommand;

    public void onEnable(Path dataDirectory) {
        // TODO: Bind to Hytale mod lifecycle.
        configService = new ConfigService(dataDirectory.resolve("config"));
        configService.loadOrCreate();

        userService = new UserService(dataDirectory.resolve("config"));
        userService.loadOrCreate();

        messageService = new MessageService(configService);
        messageService.loadLanguage();

        permissionService = new PermissionService(configService, userService);

        zBenCommand = new ZBenCommand(configService, messageService, permissionService, userService);
        zBenCommand.register();

        logInfo("ZBenEssentials loaded");
    }

    public void onPlayerJoin(String playerName) {
        if (!configService.getConfig().getWelcome().isEnabled()) {
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        for (String message : configService.getConfig().getWelcome().getMessages()) {
            sendMessage(configService.getConfig().getWelcome().isBroadcastToAll(),
                    messageService.getMessage("welcome.message", placeholders)
                            .replace("{message}", message.replace("{player}", playerName))
            );
        }
    }

    public void onPlayerJoinQuitMessage(String playerName, boolean joined) {
        if (!configService.getConfig().getJoinQuit().isEnabled()) {
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        String key = joined ? "join.message" : "quit.message";
        sendMessage(true, messageService.getMessage(key, placeholders));
    }

    public String formatChat(String playerName, String group, String message) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        placeholders.put("message", message);
        placeholders.put("prefix", configService.getConfig().getPrefix());

        return configService.getConfig().getChatFormats().values().stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .findFirst()
                .map(format -> format.getFormat())
                .orElse("{prefix} {player}: {message}")
                .replace("{prefix}", placeholders.get("prefix"))
                .replace("{player}", placeholders.get("player"))
                .replace("{message}", placeholders.get("message"));
    }

    private void logInfo(String message) {
        // TODO: Replace with Hytale logger.
        System.out.println(message);
    }

    private void sendMessage(boolean broadcast, String message) {
        // TODO: Integrate with Hytale API to send chat messages.
        if (broadcast) {
            System.out.println("[Broadcast] " + message);
        } else {
            System.out.println("[Private] " + message);
        }
    }
}
