package com.zbennoz.zbenclaims.display;

import com.zbennoz.zbenclaims.ZBenClaimsPlugin;
import com.zbennoz.zbenclaims.api.JobProvider;
import com.zbennoz.zbenclaims.api.RankProvider;
import com.zbennoz.zbenclaims.api.RankView;
import com.zbennoz.zbenclaims.api.TeamProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TabDisplayManager {
    private final ZBenClaimsPlugin plugin;
    private final Scoreboard scoreboard;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    private final Map<UUID, Long> lastUpdate = new ConcurrentHashMap<>();
    private final long debounceMillis = 500L;

    public TabDisplayManager(ZBenClaimsPlugin plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void update(Player player) {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) return;
        long now = System.currentTimeMillis();
        long last = lastUpdate.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < debounceMillis) return;
        lastUpdate.put(player.getUniqueId(), now);

        RankProvider rankProvider = plugin.getService(RankProvider.class).orElse(null);
        RankView rank = rankProvider != null ? rankProvider.getRank(player.getUniqueId()) : null;
        String team = plugin.getService(TeamProvider.class).map(p -> p.getTeam(player.getUniqueId())).orElse(null);
        String job = plugin.getService(JobProvider.class).map(p -> p.getJob(player.getUniqueId())).orElse(null);

        StringBuilder prefix = new StringBuilder();
        if (rank != null && rank.tabPrefix() != null) {
            prefix.append(rank.tabPrefix());
        }
        if (team != null && !team.isBlank()) {
            prefix.append("[").append(team).append("] ");
        }
        Component name = serializer.deserialize(prefix + player.getName());
        player.playerListName(name);

        String suffixRaw = job != null && !job.isBlank() ? " «" + job + "»" : "";
        Team sbTeam = ensureTeam(rank);
        sbTeam.prefix(serializer.deserialize(prefix.toString()));
        sbTeam.suffix(serializer.deserialize(suffixRaw));
        sbTeam.addEntry(player.getName());
    }

    private Team ensureTeam(RankView rank) {
        String teamName = rank != null ? ("rank_" + rank.key()).toLowerCase() : "rank_default";
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        return team;
    }

    public void clear(Player player) {
        scoreboard.getTeams().forEach(team -> team.removeEntry(player.getName()));
        lastUpdate.remove(player.getUniqueId());
    }
}
