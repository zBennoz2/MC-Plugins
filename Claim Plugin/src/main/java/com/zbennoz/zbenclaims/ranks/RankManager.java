package com.zbennoz.zbenclaims.ranks;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import com.zbennoz.zbenclaims.db.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

public class RankManager implements Listener {

    private final ZBenClaimsPlugin plugin;
    private final Database db;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    private Map<String, Rank> ranks = new HashMap<>();
    private List<Rank> sorted = new ArrayList<>();
    private String mode = "auto";

    public RankManager(ZBenClaimsPlugin plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
        reload();
    }

    public void reload() {
        this.mode = plugin.getConfig().getString("ranks.mode", "auto").toLowerCase(Locale.ROOT);

        var section = plugin.getConfig().getConfigurationSection("ranks.list");
        if (section == null) {
            ranks = new HashMap<>();
            sorted = new ArrayList<>();
            return;
        }

        Map<String, Rank> tmp = new HashMap<>();
        for (String name : section.getKeys(false)) {
            String path = "ranks.list." + name + ".";
            int prio = plugin.getConfig().getInt(path + "priority", 0);
            int limit = plugin.getConfig().getInt(path + "limit", 10);
            String tab = plugin.getConfig().getString(path + "tabPrefix", "");
            String chat = plugin.getConfig().getString(path + "chatPrefix", "");
            String tag = plugin.getConfig().getString(path + "nametagPrefix", "");
            double cost = plugin.getConfig().getDouble(path + "cost", 0.0D);

            Set<String> flags = Optional.ofNullable(plugin.getConfig().getConfigurationSection(path + "flags"))
                    .map(flagSec -> flagSec.getValues(false).entrySet().stream()
                            .filter(e -> e.getValue() instanceof Boolean && (Boolean) e.getValue())
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet()))
                    .orElseGet(HashSet::new);

            Set<String> permissions = Optional.ofNullable(plugin.getConfig().getConfigurationSection(path + "permissions"))
                    .map(permSec -> permSec.getValues(false).entrySet().stream()
                            .filter(e -> e.getValue() instanceof Boolean && (Boolean) e.getValue())
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet()))
                    .orElseGet(HashSet::new);

            tmp.put(name.toLowerCase(Locale.ROOT), new Rank(name, prio, limit, tab, chat, tag, cost, flags, permissions));
        }
        this.ranks = tmp;
        this.sorted = tmp.values().stream()
                .sorted(Comparator.comparingInt(Rank::priority).reversed())
                .collect(Collectors.toList());

        Bukkit.getOnlinePlayers().forEach(this::applyVisuals);
    }

    public Rank getRank(UUID uuid, Player onlineIfAny) {
        if (mode.equals("database") || mode.equals("auto")) {
            String r = db.getPlayerRank(uuid);
            if (r != null) {
                Rank rank = ranks.get(r.toLowerCase(Locale.ROOT));
                if (rank != null) return rank;
            }
            if (mode.equals("database")) return fallbackRank();
        }

        if (mode.equals("permission") || mode.equals("auto")) {
            Player p = onlineIfAny;
            if (p != null) {
                for (Rank rank : sorted) {
                    if (p.hasPermission("zbenclaims.rank." + rank.name())) return rank;
                }
            } else {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.isOnline() && op.getPlayer() != null) {
                    Player pl = op.getPlayer();
                    for (Rank rank : sorted) {
                        if (pl.hasPermission("zbenclaims.rank." + rank.name())) return rank;
                    }
                }
            }
            if (mode.equals("permission")) return fallbackRank();
        }

        return fallbackRank();
    }

    private Rank fallbackRank() {
        return sorted.isEmpty()
                ? new Rank("Default", 0, 10, "", "", "", 0.0D, Set.of(), Set.of())
                : sorted.get(sorted.size() - 1);
    }

    public int getClaimLimit(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        Rank r = getRank(uuid, p);
        return Math.max(0, r.limit());
    }

    public Optional<Rank> getRank(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(ranks.get(name.toLowerCase(Locale.ROOT)));
    }

    public Collection<Rank> getRanks() {
        return Collections.unmodifiableCollection(sorted);
    }

    public void applyVisuals(Player p) {
        Rank r = getRank(p.getUniqueId(), p);

        p.playerListName(serializer.deserialize(r.tabPrefix() + p.getName()));

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = makeTeamName(r);
        Team team = sb.getTeam(teamName);
        if (team == null) team = sb.registerNewTeam(teamName);

        team.prefix(serializer.deserialize(r.nametagPrefix()));
        team.suffix(Component.empty());

        for (Team t : sb.getTeams()) {
            if (t.equals(team)) continue;
            if (t.getName().startsWith("ZBC_") && t.hasEntry(p.getName())) t.removeEntry(p.getName());
        }
        team.addEntry(p.getName());
    }

    private String makeTeamName(Rank r) {
        int prio = Math.max(0, Math.min(999, r.priority()));
        String base = "ZBC_" + String.format("%03d", prio) + "_";
        String n = r.name().replaceAll("[^A-Za-z0-9]", "");
        if (n.length() > 10) n = n.substring(0, 10);
        String name = base + n;
        if (name.length() > 16) name = name.substring(0, 16);
        return name;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        applyVisuals(e.getPlayer());
    }
}
