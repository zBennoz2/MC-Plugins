package com.zbennoz.zbencore.teams;

import com.zbennoz.zbencore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TeamConversationManager implements Listener {

    private static final int TIMEOUT_TICKS = 20 * 120; // 2 minutes

    private final JavaPlugin plugin;
    private final TeamService teamService;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public TeamConversationManager(JavaPlugin plugin, TeamService teamService) {
        this.plugin = plugin;
        this.teamService = teamService;
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void startCreate(Player player) {
        if (hasSession(player)) {
            player.sendMessage(Msg.pref(plugin, "&cDu bist bereits in einem Team-Dialog. Tippe &e/cancel &cum abzubrechen."));
            return;
        }
        Session session = new Session(SessionType.CREATE, player, null);
        sessions.put(player.getUniqueId(), session);
        session.prompt();
    }

    public void startEdit(Player player, String initialKey) {
        if (hasSession(player)) {
            player.sendMessage(Msg.pref(plugin, "&cDu bist bereits in einem Team-Dialog. Tippe &e/cancel &cum abzubrechen."));
            return;
        }
        if (initialKey != null && !teamService.exists(initialKey)) {
            player.sendMessage(Msg.pref(plugin, "&cDieses Team gibt es nicht."));
            return;
        }
        Session session = new Session(SessionType.EDIT, player, initialKey);
        sessions.put(player.getUniqueId(), session);
        session.prompt();
    }

    public void endSession(Player player, String message) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancelTimeout();
        }
        if (message != null) {
            player.sendMessage(Msg.pref(plugin, message));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> session.handleInput(input));
    }

    private final class Session {
        private final SessionType type;
        private final Player player;
        private String workingKey;
        private String displayName;
        private String prefix;
        private String color;
        private Integer weight;
        private int step;
        private BukkitTask timeoutTask;

        Session(SessionType type, Player player, String initialKey) {
            this.type = type;
            this.player = player;
            this.workingKey = initialKey;
            this.step = (type == SessionType.EDIT && initialKey != null) ? 1 : 0;
            scheduleTimeout();
        }

        void prompt() {
            scheduleTimeout();
            if (type == SessionType.CREATE) {
                switch (step) {
                    case 0 -> player.sendMessage(Msg.pref(plugin, "&7Erstelle neues Team. &eEindeutigen Schlüssel eingeben (z.B. rot)."));
                    case 1 -> player.sendMessage(Msg.pref(plugin, "&7Anzeigenamen eingeben (z.B. Moderator)."));
                    case 2 -> player.sendMessage(Msg.pref(plugin, "&7Prefix eingeben (Farbcodes mit &)."));
                    case 3 -> player.sendMessage(Msg.pref(plugin, "&7Farbe eingeben (z.B. GREEN)."));
                    case 4 -> player.sendMessage(Msg.pref(plugin, "&7Priorität/Weight eingeben (Zahl, höher = wichtiger)."));
                    default -> finishCreate();
                }
            } else {
                Team current = safeTeam();
                if (current == null) {
                    endSession(player, "&cTeam nicht gefunden. Dialog beendet.");
                    return;
                }
                switch (step) {
                    case 0 -> {
                        String list = String.join(", ", teamService.listSorted().stream().map(Team::getKey).toList());
                        player.sendMessage(Msg.pref(plugin, "&7Welches Team bearbeiten? Vorhandene Keys: &e" + (list.isEmpty() ? "&ckeine" : list)));
                        player.sendMessage(Msg.pref(plugin, "&7Team-Key eingeben (z.B. rot)."));
                        break;
                    }
                    case 1 -> player.sendMessage(Msg.pref(plugin, "&7Neuen Anzeigenamen eingeben (leer = unverändert)."));
                    case 2 -> player.sendMessage(Msg.pref(plugin, "&7Neuen Prefix eingeben (leer = unverändert)."));
                    case 3 -> player.sendMessage(Msg.pref(plugin, "&7Neue Farbe eingeben (z.B. GREEN, leer = unverändert)."));
                    case 4 -> player.sendMessage(Msg.pref(plugin, "&7Neue Priorität/Weight eingeben (leer = unverändert)."));
                    default -> finishEdit();
                }
            }
        }

        void handleInput(String input) {
            if ("cancel".equalsIgnoreCase(input)) {
                endSession(player, "&cDialog abgebrochen.");
                return;
            }

            if (type == SessionType.CREATE) {
                handleCreate(input);
            } else {
                handleEdit(input);
            }
        }

        private void handleCreate(String input) {
            switch (step) {
                case 0 -> {
                    String key = input.toLowerCase(Locale.ROOT).replace(" ", "");
                    if (key.isBlank()) {
                        player.sendMessage(Msg.pref(plugin, "&cKey darf nicht leer sein."));
                        return;
                    }
                    if (teamService.exists(key)) {
                        player.sendMessage(Msg.pref(plugin, "&cDiesen Key gibt es bereits."));
                        return;
                    }
                    this.workingKey = key;
                    step++;
                    prompt();
                }
                case 1 -> {
                    if (input.isBlank()) {
                        player.sendMessage(Msg.pref(plugin, "&cAnzeigename darf nicht leer sein."));
                        return;
                    }
                    this.displayName = input;
                    step++;
                    prompt();
                }
                case 2 -> {
                    this.prefix = input;
                    step++;
                    prompt();
                }
                case 3 -> {
                    ChatColor cc = teamService.parseColor(input);
                    this.color = teamService.normalizeColor(input);
                    player.sendMessage(Msg.pref(plugin, "&7Genutzte Farbe: " + cc + cc.name()));
                    step++;
                    prompt();
                }
                case 4 -> {
                    try {
                        this.weight = Integer.parseInt(input);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(Msg.pref(plugin, "&cBitte gib eine Zahl an."));
                        return;
                    }
                    finishCreate();
                }
                default -> finishCreate();
            }
        }

        private void handleEdit(String input) {
            switch (step) {
                case 0 -> {
                    if (input.isBlank()) {
                        player.sendMessage(Msg.pref(plugin, "&cBitte gib einen Team-Key ein."));
                        return;
                    }
                    if (!teamService.exists(input)) {
                        player.sendMessage(Msg.pref(plugin, "&cTeam nicht gefunden."));
                        return;
                    }
                    workingKey = input;
                    step++;
                    prompt();
                }
                case 1 -> {
                    if (!input.isBlank()) {
                        displayName = input;
                    }
                    step++;
                    prompt();
                }
                case 2 -> {
                    if (!input.isBlank()) {
                        prefix = input;
                    }
                    step++;
                    prompt();
                }
                case 3 -> {
                    if (!input.isBlank()) {
                        ChatColor cc = teamService.parseColor(input);
                        color = teamService.normalizeColor(input);
                        player.sendMessage(Msg.pref(plugin, "&7Genutzte Farbe: " + cc + cc.name()));
                    }
                    step++;
                    prompt();
                }
                case 4 -> {
                    if (!input.isBlank()) {
                        try {
                            weight = Integer.parseInt(input);
                        } catch (NumberFormatException ex) {
                            player.sendMessage(Msg.pref(plugin, "&cBitte gib eine Zahl an."));
                            return;
                        }
                    }
                    finishEdit();
                }
                default -> finishEdit();
            }
        }

        private void finishCreate() {
            if (workingKey == null || displayName == null || prefix == null || color == null || weight == null) {
                player.sendMessage(Msg.pref(plugin, "&cDialog unvollständig, bitte erneut versuchen."));
                endSession(player, null);
                return;
            }
            Team team = new Team(workingKey, displayName, prefix, color, weight);
            teamService.add(team);
            endSession(player, "&aTeam &e" + workingKey + " &awurde erstellt und gespeichert.");
        }

        private void finishEdit() {
            Team current = safeTeam();
            if (current == null) {
                endSession(player, "&cTeam nicht mehr vorhanden.");
                return;
            }
            String newDisplay = displayName != null ? displayName : current.getDisplayName();
            String newPrefix = prefix != null ? prefix : current.getPrefix();
            String newColor = color != null ? color : current.getColor();
            int newWeight = weight != null ? weight : current.getWeight();

            Team updated = new Team(current.getKey(), newDisplay, newPrefix, newColor, newWeight);
            teamService.update(current.getKey(), updated);
            endSession(player, "&aTeam &e" + current.getKey() + " &awurde aktualisiert.");
        }

        private Team safeTeam() {
            if (workingKey == null) return null;
            return teamService.get(workingKey);
        }

        private void scheduleTimeout() {
            cancelTimeout();
            timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (sessions.remove(player.getUniqueId()) != null) {
                    player.sendMessage(Msg.pref(plugin, "&cDialog wegen Inaktivität beendet."));
                }
            }, TIMEOUT_TICKS);
        }

        private void cancelTimeout() {
            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
        }
    }

    private enum SessionType {
        CREATE, EDIT
    }
}
