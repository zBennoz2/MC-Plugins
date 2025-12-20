package com.zbennoz.zbencore.ranks;

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

public final class RankConversationManager implements Listener {

    private static final int TIMEOUT_TICKS = 20 * 120; // 2 minutes

    private final JavaPlugin plugin;
    private final RankService rankService;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public RankConversationManager(JavaPlugin plugin, RankService rankService) {
        this.plugin = plugin;
        this.rankService = rankService;
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void startCreate(Player player) {
        if (hasSession(player)) {
            player.sendMessage(Msg.pref(plugin, "&cDu bist bereits in einem Rang-Dialog. Tippe &e/cancel &cum abzubrechen."));
            return;
        }
        Session session = new Session(SessionType.CREATE, player, null);
        sessions.put(player.getUniqueId(), session);
        session.prompt();
    }

    public void startEdit(Player player, String initialKey) {
        if (hasSession(player)) {
            player.sendMessage(Msg.pref(plugin, "&cDu bist bereits in einem Rang-Dialog. Tippe &e/cancel &cum abzubrechen."));
            return;
        }
        if (initialKey != null && !rankService.exists(initialKey)) {
            player.sendMessage(Msg.pref(plugin, "&cDiesen Rang gibt es nicht."));
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
                    case 0 -> player.sendMessage(Msg.pref(plugin, "&7Erstelle neuen Rang. &eEindeutigen Schlüssel eingeben (z.B. moderator)."));
                    case 1 -> player.sendMessage(Msg.pref(plugin, "&7Anzeigenamen eingeben (z.B. Moderator)."));
                    case 2 -> player.sendMessage(Msg.pref(plugin, "&7Prefix eingeben (Farbcodes mit &)."));
                    case 3 -> player.sendMessage(Msg.pref(plugin, "&7Farbe eingeben (z.B. GREEN oder #00FF00)."));
                    case 4 -> player.sendMessage(Msg.pref(plugin, "&7Priorität/Weight eingeben (Zahl, höher = wichtiger)."));
                    default -> finishCreate();
                }
            } else {
                Rank current = safeRank();
                if (current == null) {
                    endSession(player, "&cRang nicht gefunden. Dialog beendet.");
                    return;
                }
                switch (step) {
                    case 0 -> {
                        String list = String.join(", ", rankService.listSorted().stream().map(Rank::getKey).toList());
                        player.sendMessage(Msg.pref(plugin, "&7Welchen Rang bearbeiten? Vorhandene Keys: &e" + (list.isEmpty() ? "-" : list)));
                    }
                    case 1 -> player.sendMessage(Msg.pref(plugin, "&7Neuer Anzeigename (leer = bleibt). Aktuell: &e" + current.getDisplayName()));
                    case 2 -> player.sendMessage(Msg.pref(plugin, "&7Neues Prefix (leer = bleibt). Aktuell: &e" + current.getPrefix()));
                    case 3 -> player.sendMessage(Msg.pref(plugin, "&7Neue Farbe (leer = bleibt). Aktuell: &e" + current.getColor()));
                    case 4 -> player.sendMessage(Msg.pref(plugin, "&7Neuer Weight (Zahl, leer = bleibt). Aktuell: &e" + current.getWeight()));
                    default -> finishEdit();
                }
            }
        }

        void handleInput(String input) {
            scheduleTimeout();

            if ("/cancel".equalsIgnoreCase(input) || "cancel".equalsIgnoreCase(input)) {
                endSession(player, "&cRang-Dialog abgebrochen.");
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
                    if (rankService.exists(key)) {
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
                    ChatColor cc = rankService.parseColor(input);
                    this.color = rankService.normalizeColor(input);
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
                        player.sendMessage(Msg.pref(plugin, "&cBitte gib einen Rang-Key ein."));
                        return;
                    }
                    if (!rankService.exists(input)) {
                        player.sendMessage(Msg.pref(plugin, "&cRang nicht gefunden."));
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
                        ChatColor cc = rankService.parseColor(input);
                        color = rankService.normalizeColor(input);
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
            Rank rank = new Rank(workingKey, displayName, prefix, color, weight);
            rankService.add(rank);
            endSession(player, "&aRang &e" + workingKey + " &awurde erstellt und gespeichert.");
        }

        private void finishEdit() {
            Rank current = safeRank();
            if (current == null) {
                endSession(player, "&cRang nicht mehr vorhanden.");
                return;
            }
            String newDisplay = displayName != null ? displayName : current.getDisplayName();
            String newPrefix = prefix != null ? prefix : current.getPrefix();
            String newColor = color != null ? color : current.getColor();
            int newWeight = weight != null ? weight : current.getWeight();

            Rank updated = new Rank(current.getKey(), newDisplay, newPrefix, newColor, newWeight);
            rankService.update(current.getKey(), updated);
            endSession(player, "&aRang &e" + current.getKey() + " &awurde aktualisiert.");
        }

        private Rank safeRank() {
            if (workingKey == null) return null;
            return rankService.get(workingKey);
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
