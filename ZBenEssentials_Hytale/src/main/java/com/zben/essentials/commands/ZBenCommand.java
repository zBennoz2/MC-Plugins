package com.zben.essentials.commands;

import com.zben.essentials.model.Home;
import com.zben.essentials.model.InteractiveMessage;
import com.zben.essentials.model.PlayerLocation;
import com.zben.essentials.model.TpaRequest;
import com.zben.essentials.model.Warp;
import com.zben.essentials.services.BackService;
import com.zben.essentials.services.ConfigService;
import com.zben.essentials.services.HomeService;
import com.zben.essentials.services.MessageService;
import com.zben.essentials.services.PermissionService;
import com.zben.essentials.services.SpawnService;
import com.zben.essentials.services.TpaService;
import com.zben.essentials.services.UserService;
import com.zben.essentials.services.WarpService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    private final BackService backService;
    private final SpawnService spawnService;
    private final WarpService warpService;
    private final TpaService tpaService;

    public ZBenCommand(ConfigService configService,
                       MessageService messageService,
                       PermissionService permissionService,
                       UserService userService,
                       HomeService homeService,
                       BackService backService,
                       SpawnService spawnService,
                       WarpService warpService,
                       TpaService tpaService) {
        this.configService = configService;
        this.messageService = messageService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.homeService = homeService;
        this.backService = backService;
        this.spawnService = spawnService;
        this.warpService = warpService;
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
        if (configService.getConfig().getBack().isEnabled()) {
            backService.setBackLocation(context.getSenderId(), context.getSenderLocation());
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
        String requestText = messageService.getMessage("tpa_request_received", targetPlaceholders);
        if (context.supportsChatComponents()) {
            List<InteractiveMessage.Action> actions = new ArrayList<>();
            actions.add(new InteractiveMessage.Action(
                    messageService.getMessage("tpa_accept_button"),
                    "/tpaccept",
                    messageService.getMessage("tpa_accept_tooltip")
            ));
            actions.add(new InteractiveMessage.Action(
                    messageService.getMessage("tpa_deny_button"),
                    "/tpdeny",
                    messageService.getMessage("tpa_deny_tooltip")
            ));
            context.sendInteractiveMessageTo(request.getTargetId(), new InteractiveMessage(requestText, actions));
        } else {
            context.sendMessageTo(request.getTargetId(), requestText);
            context.sendMessageTo(request.getTargetId(), messageService.getMessage("tpa_request_fallback"));
        }
    }

    public void handleTpaAccept(TpaCommandContext context) {
        if (!permissionService.hasPermission(context.getSenderId(), "zben.tpa.respond")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        Duration timeout = getTpaTimeout();
        TpaService.RequestOutcome outcome = tpaService.acceptRequest(context.getSenderId(), timeout);
        if (outcome.getStatus() == TpaService.RequestStatus.NOT_FOUND) {
            context.sendMessage(messageService.getMessage("tpa_request_missing"));
            return;
        }
        if (outcome.getStatus() == TpaService.RequestStatus.EXPIRED) {
            context.sendMessage(messageService.getMessage("tpa_request_expired"));
            return;
        }
        Optional<TpaRequest> request = outcome.getRequest();
        if (request.isEmpty()) {
            context.sendMessage(messageService.getMessage("tpa_request_missing"));
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
        if (configService.getConfig().getBack().isEnabled()) {
            backService.setBackLocation(teleportPlayer, context.getPlayerLocation(teleportPlayer));
        }
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
        TpaService.RequestOutcome outcome = tpaService.denyRequest(context.getSenderId(), timeout);
        if (outcome.getStatus() == TpaService.RequestStatus.NOT_FOUND) {
            context.sendMessage(messageService.getMessage("tpa_request_missing"));
            return;
        }
        if (outcome.getStatus() == TpaService.RequestStatus.EXPIRED) {
            context.sendMessage(messageService.getMessage("tpa_request_expired"));
            return;
        }
        Optional<TpaRequest> request = outcome.getRequest();
        if (request.isEmpty()) {
            context.sendMessage(messageService.getMessage("tpa_request_missing"));
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

    public void handleBack(HomeCommandContext context) {
        if (!configService.getConfig().getBack().isEnabled()) {
            context.sendMessage(messageService.getMessage("error.feature_disabled"));
            return;
        }
        if (!permissionService.hasPermission(context.getSenderId(), "zben.back")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        Optional<PlayerLocation> location = backService.getBackLocation(context.getSenderId());
        if (location.isEmpty()) {
            context.sendMessage(messageService.getMessage("back_none"));
            return;
        }
        context.teleportSender(location.get());
        context.sendMessage(messageService.getMessage("back_success"));
    }

    public void handleSetSpawn(HomeCommandContext context) {
        if (!configService.getConfig().getSpawn().isEnabled()) {
            context.sendMessage(messageService.getMessage("error.feature_disabled"));
            return;
        }
        if (!permissionService.hasPermission(context.getSenderId(), "zben.spawn.set")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        spawnService.setSpawn(context.getSenderLocation());
        context.sendMessage(messageService.getMessage("spawn_set"));
    }

    public void handleSpawn(HomeCommandContext context) {
        if (!configService.getConfig().getSpawn().isEnabled()) {
            context.sendMessage(messageService.getMessage("error.feature_disabled"));
            return;
        }
        if (!permissionService.hasPermission(context.getSenderId(), "zben.spawn.use")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        Optional<PlayerLocation> spawn = spawnService.getSpawn();
        if (spawn.isEmpty()) {
            context.sendMessage(messageService.getMessage("spawn_no_spawn"));
            return;
        }
        context.teleportSender(spawn.get());
        context.sendMessage(messageService.getMessage("spawn_tp"));
    }

    public void handleSetWarp(HomeCommandContext context, String name) {
        if (!configService.getConfig().getWarps().isEnabled()) {
            context.sendMessage(messageService.getMessage("error.feature_disabled"));
            return;
        }
        if (!permissionService.hasPermission(context.getSenderId(), "zben.warp.set")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        if (!warpService.isValidWarpName(name)) {
            context.sendMessage(messageService.getMessage("warp_invalid_name"));
            return;
        }
        if (warpService.hasWarp(name)) {
            context.sendMessage(messageService.getMessage("warp_exists"));
            return;
        }
        PlayerLocation location = context.getSenderLocation();
        Warp warp = new Warp(
                location.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                Instant.now().toString()
        );
        warpService.setWarp(name, warp);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        context.sendMessage(messageService.getMessage("warp_set", placeholders));
    }

    public void handleWarp(HomeCommandContext context, String name) {
        if (!configService.getConfig().getWarps().isEnabled()) {
            context.sendMessage(messageService.getMessage("error.feature_disabled"));
            return;
        }
        if (!permissionService.hasPermission(context.getSenderId(), "zben.warp.use")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        if (!warpService.isValidWarpName(name)) {
            context.sendMessage(messageService.getMessage("warp_invalid_name"));
            return;
        }
        Warp warp = warpService.getWarp(name);
        if (warp == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("warp_not_found", placeholders));
            return;
        }
        context.teleportSender(toLocation(warp));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        context.sendMessage(messageService.getMessage("warp_tp", placeholders));
    }

    public void handleDelWarp(HomeCommandContext context, String name) {
        if (!configService.getConfig().getWarps().isEnabled()) {
            context.sendMessage(messageService.getMessage("error.feature_disabled"));
            return;
        }
        if (!permissionService.hasPermission(context.getSenderId(), "zben.warp.del")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        if (!warpService.isValidWarpName(name)) {
            context.sendMessage(messageService.getMessage("warp_invalid_name"));
            return;
        }
        if (!warpService.removeWarp(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            context.sendMessage(messageService.getMessage("warp_not_found", placeholders));
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        context.sendMessage(messageService.getMessage("warp_deleted", placeholders));
    }

    public void handleWarps(HomeCommandContext context) {
        if (!configService.getConfig().getWarps().isEnabled()) {
            context.sendMessage(messageService.getMessage("error.feature_disabled"));
            return;
        }
        if (!permissionService.hasPermission(context.getSenderId(), "zben.warp.list")) {
            context.sendMessage(messageService.getMessage("error.no_permission"));
            return;
        }
        List<String> warps = warpService.listWarps();
        warps.sort(Comparator.naturalOrder());
        if (warps.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("warps", "-");
            context.sendMessage(messageService.getMessage("warp_list_title", placeholders));
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warps", String.join(", ", warps));
        String listTitle = messageService.getMessage("warp_list_title", placeholders);
        if (context.supportsChatComponents()) {
            List<InteractiveMessage.Action> actions = new ArrayList<>();
            for (String warpName : warps) {
                actions.add(new InteractiveMessage.Action(
                        warpName,
                        "/warp " + warpName,
                        messageService.getMessage("warp_tp_hover")
                ));
            }
            context.sendInteractiveMessage(new InteractiveMessage(listTitle, actions));
        } else {
            context.sendMessage(listTitle);
        }
    }

    public interface CommandContext {
        UUID getSenderId();
        void sendMessage(String message);
        boolean supportsChatComponents();
        void sendInteractiveMessage(InteractiveMessage message);
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
        void sendInteractiveMessageTo(UUID playerId, InteractiveMessage message);
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

    private PlayerLocation toLocation(Warp warp) {
        return new PlayerLocation(
                warp.getWorld(),
                warp.getX(),
                warp.getY(),
                warp.getZ(),
                warp.getYaw(),
                warp.getPitch()
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
