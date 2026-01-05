package com.zbennoz.zbenadmintool.util;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModerationService implements Listener {

    private final ZBenAdmintool plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, String> muted = new HashMap<>();

    public ModerationService(ZBenAdmintool plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "moderation.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadMutes();
    }

    private void loadMutes() {
        if (!config.isConfigurationSection("mutes")) {
            return;
        }
        for (String key : config.getConfigurationSection("mutes").getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            muted.put(uuid, config.getString("mutes." + key + ".reason", "Stummgeschaltet"));
        }
    }

    public void mute(UUID uuid, String reason) {
        muted.put(uuid, reason);
        config.set("mutes." + uuid + ".reason", reason);
        save();
    }

    public void unmute(UUID uuid) {
        muted.remove(uuid);
        config.set("mutes." + uuid, null);
        save();
    }

    public boolean isMuted(UUID uuid) {
        return muted.containsKey(uuid);
    }

    public String getMuteReason(UUID uuid) {
        return muted.get(uuid);
    }

    public void addWarning(UUID uuid, String issuer, String reason) {
        List<String> warnings = config.getStringList("warnings." + uuid);
        warnings.add(Instant.now() + " | " + issuer + ": " + reason);
        config.set("warnings." + uuid, warnings);
        save();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            String reason = muted.get(player.getUniqueId());
            player.sendMessage("§cDu bist stummgeschaltet: " + reason);
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte moderation.yml nicht speichern: " + e.getMessage());
        }
    }
}
