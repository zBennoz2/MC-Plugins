package com.zben.essentials.commands;

import com.zben.essentials.model.Home;
import com.zben.essentials.model.PlayerLocation;
import com.zben.essentials.model.TpaRequest;
import com.zben.essentials.services.ConfigService;
import com.zben.essentials.services.HomeService;
import com.zben.essentials.services.MessageService;
import com.zben.essentials.services.PermissionService;
import com.zben.essentials.services.TpaService;
import com.zben.essentials.services.UserService;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ZBenCommand {
    private final ConfigService configService;
    private final MessageService messageService;
    private final PermissionService permissionService;
    private final UserService userService;
    private final HomeService homeService;
    private final TpaService tpaService;

    public ZBenCommand(ConfigService configService,
                       MessageService messageService,
                       PermissionService permissionService,
                       UserService userService,
                       HomeService homeService,
                       TpaService tpaService) {
        this.configService = configService;
        this.messageService = messageService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.homeService = homeService;
        this.tpaService = tpaService;
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

    public void handleSetHome(HomeCommandContext context, String name) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.home.sethome")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        if (!homeService.isValidHomeName(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("error.home_invalid_name", placeholders));
            return;
        }
        if (homeService.hasHome(context.getSenderId(), name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("error.home_exists", placeholders));
            return;
        }
        int limit = resolveHomeLimit(context.getSenderId());
        int current = homeService.countHomes(context.getSenderId());
        if (limit >= 0 && current >= limit) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("limit", String.valueOf(limit));
            context.sendMessage(messageService.getMessage("error.home_limit", placeholders));
            return;
        }
        PlayerLocation location = context.getSenderLocation();
        Home home = new Home(
                location.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                Instant.now().toString()
        );
        homeService.setHome(context.getSenderId(), name, home);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        context.sendMessage(messageService.getMessage("command.sethome", placeholders));
    }

    public void handleHome(HomeCommandContext context, String name) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.home.use")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        if (!homeService.isValidHomeName(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("error.home_invalid_name", placeholders));
            return;
        }
        Home home = homeService.getHome(context.getSenderId(), name);
        if (home == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("error.home_not_found", placeholders));
            return;
        }
        context.teleportSender(toLocation(home));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        context.sendMessage(messageService.getMessage("command.home", placeholders));
    }

    public void handleHomes(HomeCommandContext context) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.home.list")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        List<String> homes = homeService.listHomes(context.getSenderId());
        homes.sort(Comparator.naturalOrder());
        if (homes.isEmpty()) {
            context.sendMessage(messageService.getMessage("command.homes_empty"));
            return;
        }
        int limit = resolveHomeLimit(context.getSenderId());
        String limitLabel = limit < 0 ? "∞" : String.valueOf(limit);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("homes", String.join(", ", homes));
        placeholders.put("count", String.valueOf(homes.size()));
        placeholders.put("limit", limitLabel);
        context.sendMessage(messageService.getMessage("command.homes", placeholders));
    }

    public void handleDelHome(HomeCommandContext context, String name) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.home.del")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        if (!homeService.isValidHomeName(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("error.home_invalid_name", placeholders));
            return;
        }
        if (!homeService.removeHome(context.getSenderId(), name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("error.home_not_found", placeholders));
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        context.sendMessage(messageService.getMessage("command.delhome", placeholders));
    }

    public void handleTpaRequest(TpaCommandContext context, String targetName, boolean teleportHere) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.tpa.request")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        Optional<PlayerInfo> target = context.findOnlinePlayer(targetName);
        if (target.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            context.sendMessage(messageService.getMessage("error.tpa_player_offline", placeholders));
            return;
        }
        PlayerInfo targetInfo = target.get();
        if (context.getSenderId().equals(targetInfo.getId())) {
            context.sendMessage(messageService.getMessage("error.tpa_self"));
            return;
        }
        Duration cooldown = getTpaCooldown();
        Optional<Duration> remaining = tpaService.getCooldownRemaining(context.getSenderId(), cooldown);
        if (remaining.isPresent()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("seconds", String.valueOf(remaining.get().toSeconds()));
            context.sendMessage(messageService.getMessage("error.tpa_cooldown", placeholders));
            return;
        }
        Duration timeout = getTpaTimeout();
        if (tpaService.hasActiveRequest(context.getSenderId(), timeout)
                || tpaService.hasActiveRequest(targetInfo.getId(), timeout)) {
            context.sendMessage(messageService.getMessage("error.tpa_request_exists"));
            return;
        }
        TpaRequest request = tpaService.createRequest(
                context.getSenderId(),
                context.getSenderName(),
                targetInfo.getId(),
                targetInfo.getName(),
                teleportHere
        );
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", request.getTargetName());
        context.sendMessage(messageService.getMessage(
                teleportHere ? "command.tpahere.sent" : "command.tpa.sent",
                placeholders
        ));
        Map<String, String> targetPlaceholders = new HashMap<>();
        targetPlaceholders.put("player", request.getSenderName());
        context.sendMessageTo(request.getTargetId(), messageService.getMessage(
                teleportHere ? "command.tpahere.received" : "command.tpa.received",
                targetPlaceholders
        ));
    }

    public void handleTpaAccept(TpaCommandContext context) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.tpa.respond")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        Duration timeout = getTpaTimeout();
        Optional<TpaRequest> request = tpaService.acceptRequest(context.getSenderId(), timeout);
        if (request.isEmpty()) {
            context.sendMessage(messageService.getMessage("error.tpa_no_request"));
            return;
        }
        TpaRequest resolved = request.get();
        if (!context.isPlayerOnline(resolved.getSenderId())) {
            context.sendMessage(messageService.getMessage("error.tpa_sender_offline"));
            return;
        }
        PlayerLocation destination = resolved.isTeleportHere()
                ? context.getPlayerLocation(resolved.getSenderId())
                : context.getPlayerLocation(resolved.getTargetId());
        UUID teleportPlayer = resolved.isTeleportHere() ? resolved.getTargetId() : resolved.getSenderId();
        context.teleportPlayer(teleportPlayer, destination);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", resolved.getSenderName());
        context.sendMessage(messageService.getMessage("command.tpa.accepted.target", placeholders));
        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("player", resolved.getTargetName());
        context.sendMessageTo(resolved.getSenderId(),
                messageService.getMessage("command.tpa.accepted.sender", senderPlaceholders));
    }

    public void handleTpaDeny(TpaCommandContext context) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.tpa.respond")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        Duration timeout = getTpaTimeout();
        Optional<TpaRequest> request = tpaService.denyRequest(context.getSenderId(), timeout);
        if (request.isEmpty()) {
            context.sendMessage(messageService.getMessage("error.tpa_no_request"));
            return;
        }
        TpaRequest resolved = request.get();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", resolved.getSenderName());
        context.sendMessage(messageService.getMessage("command.tpa.denied.target", placeholders));
        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("player", resolved.getTargetName());
        context.sendMessageTo(resolved.getSenderId(),
                messageService.getMessage("command.tpa.denied.sender", senderPlaceholders));
    }

    public interface CommandContext {
        UUID getSenderId();
        void sendMessage(String message);
    }

    public interface HomeCommandContext extends CommandContext {
        String getSenderName();
        PlayerLocation getSenderLocation();
        void teleportSender(PlayerLocation location);
    }

    public interface TpaCommandContext extends CommandContext {
        String getSenderName();
        Optional<PlayerInfo> findOnlinePlayer(String name);
        boolean isPlayerOnline(UUID playerId);
        PlayerLocation getPlayerLocation(UUID playerId);
        void teleportPlayer(UUID playerId, PlayerLocation location);
        void sendMessageTo(UUID playerId, String message);
    }

    public static class PlayerInfo {
        private final UUID id;
        private final String name;

        public PlayerInfo(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    private PlayerLocation toLocation(Home home) {
        return new PlayerLocation(
                home.getWorld(),
                home.getX(),
                home.getY(),
                home.getZ(),
                home.getYaw(),
                home.getPitch()
        );
    }

    private int resolveHomeLimit(UUID playerId) {
        String group = permissionService.resolveGroup(playerId);
        Map<String, Integer> limits = configService.getConfig().getHomeLimits();
        Integer limit = limits.get(group);
        if (limit == null) {
            limit = limits.get(configService.getConfig().getDefaultGroup());
        }
        return limit != null ? limit : 0;
    }

    private Duration getTpaTimeout() {
        int timeoutSeconds = configService.getConfig().getTpa().getTimeoutSeconds();
        return Duration.ofSeconds(Math.max(timeoutSeconds, 0));
    }

    private Duration getTpaCooldown() {
        int cooldownSeconds = configService.getConfig().getTpa().getCooldownSeconds();
        return Duration.ofSeconds(Math.max(cooldownSeconds, 0));
    }
}
