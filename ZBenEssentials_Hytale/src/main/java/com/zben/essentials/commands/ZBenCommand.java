package com.zben.essentials.commands;

import com.zben.essentials.services.ConfigService;
import com.zben.essentials.services.MessageService;
import com.zben.essentials.services.PermissionService;
import com.zben.essentials.services.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZBenCommand {
    private final ConfigService configService;
    private final MessageService messageService;
    private final PermissionService permissionService;
    private final UserService userService;

    public ZBenCommand(ConfigService configService,
                       MessageService messageService,
                       PermissionService permissionService,
                       UserService userService) {
        this.configService = configService;
        this.messageService = messageService;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    public void register() {
        // TODO: Register command with Hytale command API.
        // Expected structure: /zben ping, /zben reload, /zben whoami, /zben setgroup <player> <group>
    }

    public void handlePing(CommandContext context) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.ping")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        context.sendMessage(messageService.getMessage("command.ping"));
    }

    public void handleReload(CommandContext context) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.admin")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        configService.loadOrCreate();
        messageService.loadLanguage();
        context.sendMessage(messageService.getMessage("command.reload"));
    }

    public void handleWhoAmI(CommandContext context) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.whoami")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        String group = permissionService.resolveGroup(context.getSenderId());
        String permissions = String.join(", ",
                configService.getConfig().getGroups().get(group).getPermissions());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("group", group);
        placeholders.put("permissions", permissions);
        context.sendMessage(messageService.getMessage("command.whoami", placeholders));
    }

    public void handleSetGroup(CommandContext context, UUID targetPlayer, String group) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.admin")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        if (!configService.getConfig().getGroups().containsKey(group)) {
            context.sendMessage(messageService.getMessage("error.group_not_found"));
            return;
        }
        userService.setGroup(targetPlayer, group);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", targetPlayer.toString());
        placeholders.put("group", group);
        context.sendMessage(messageService.getMessage("command.setgroup", placeholders));
    }

    public interface CommandContext {
        UUID getSenderId();
        void sendMessage(String message);
    }
}
