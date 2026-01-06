package com.zbennoz.zbenadmintool.service;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SuspiciousMiningService implements Listener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ZBenAdmintool plugin;
    private final Map<Material, Integer> thresholds = new HashMap<>();
    private final Map<Material, Integer> minY = new HashMap<>();
    private final Map<UUID, Deque<MiningEvent>> history = new HashMap<>();
    private final List<SuspiciousEntry> suspicions = new ArrayList<>();
    private final Map<String, Long> lastTriggers = new HashMap<>();
    private File file;
    private boolean enabled;
    private int windowSeconds;
    private boolean notifyStaff;
    private String notifyPermission;
    private int storeEventsDays;
    private boolean ignoreCreative = true;
    private boolean ignoreOperators = true;

    public SuspiciousMiningService(ZBenAdmintool plugin) {
        this.plugin = plugin;
        reload();
        new BukkitRunnable() {
            @Override
            public void run() {
                prune();
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 1200L);
    }

    public void reload() {
        loadConfig();
        loadFile();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("suspiciousMining.enabled", false);
        this.windowSeconds = config.getInt("suspiciousMining.windowSeconds", 300);
        this.notifyStaff = config.getBoolean("suspiciousMining.notifyStaff", true);
        this.notifyPermission = config.getString("suspiciousMining.staffNotifyPermission", "admintool.alerts");
        this.storeEventsDays = config.getInt("suspiciousMining.storeEventsDays", 14);
        this.ignoreCreative = config.getBoolean("suspiciousMining.ignoreCreative", true);
        this.ignoreOperators = config.getBoolean("suspiciousMining.ignoreOperators", true);
        thresholds.clear();
        minY.clear();
        FileConfiguration sec = plugin.getConfig();
        if (sec.isConfigurationSection("suspiciousMining.thresholds")) {
            sec.getConfigurationSection("suspiciousMining.thresholds").getKeys(false).forEach(key -> {
                Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                if (material != null) {
                    thresholds.put(material, sec.getInt("suspiciousMining.thresholds." + key));
                }
            });
        }
        if (sec.isConfigurationSection("suspiciousMining.minY")) {
            sec.getConfigurationSection("suspiciousMining.minY").getKeys(false).forEach(key -> {
                Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                if (material != null) {
                    minY.put(material, sec.getInt("suspiciousMining.minY." + key));
                }
            });
        }
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "suspicious-mining.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Konnte suspicious-mining.yml nicht erstellen: " + e.getMessage());
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        suspicions.clear();
        List<Map<?, ?>> list = config.getMapList("entries");
        long cutoff = Instant.now().minusSeconds(storeEventsDays * 86400L).toEpochMilli();
        for (Map<?, ?> map : list) {
            try {
                String player = (String) map.get("player");
                String material = (String) map.get("material");
                long time = ((Number) map.get("time")).longValue();
                int amount = ((Number) map.get("amount")).intValue();
                String world = (String) map.get("world");
                Object yObj = map.get("y");
                int y = yObj instanceof Number number ? number.intValue() : 0;
                boolean resolved = Boolean.TRUE.equals(map.get("resolved"));
                if (time >= cutoff) {
                    suspicions.add(new SuspiciousEntry(player, material, amount, time, world, y, resolved));
                }
            } catch (Exception ignored) {
                plugin.getLogger().warning("Konnte Verdachtsfall nicht laden: " + map);
            }
        }
        suspicions.sort((a, b) -> Long.compare(b.time(), a.time()));
        save();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if (ignoreOperators && player.isOp()) return;
        if (ignoreCreative && player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        Material type = event.getBlock().getType();
        if (!thresholds.containsKey(type)) return;
        if (event.isCancelled()) return;
        MiningEvent miningEvent = new MiningEvent(System.currentTimeMillis(), type, event.getBlock().getY(), event.getBlock().getWorld().getName());
        Deque<MiningEvent> deque = history.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>());
        deque.addLast(miningEvent);
        cleanupOld(deque);
        long count = deque.stream()
                .filter(e -> e.material() == type)
                .count();
        if (count >= thresholds.get(type)) {
            Integer min = minY.get(type);
            if (min != null && event.getBlock().getY() > min) {
                return;
            }
            String key = player.getUniqueId() + ":" + type.name();
            long now = System.currentTimeMillis();
            long last = lastTriggers.getOrDefault(key, 0L);
            if (now - last < 60000L) {
                return;
            }
            lastTriggers.put(key, now);
            SuspiciousEntry entry = new SuspiciousEntry(player.getName(), type.name(), (int) count, now,
                    event.getBlock().getWorld().getName(), event.getBlock().getY(), false);
            suspicions.add(0, entry);
            save();
            notifyStaff(entry);
        }
    }

    private void cleanupOld(Deque<MiningEvent> deque) {
        long thresholdMillis = System.currentTimeMillis() - (windowSeconds * 1000L);
        while (!deque.isEmpty() && deque.peekFirst().timestamp() < thresholdMillis) {
            deque.removeFirst();
        }
    }

    private void notifyStaff(SuspiciousEntry entry) {
        if (!notifyStaff || notifyPermission == null || notifyPermission.isEmpty()) return;
        String message = "§cVerdächtiges Mining: §f" + entry.player() + " §7- §b" + entry.material() + " §7x" + entry.amount() +
                " @ " + entry.world() + ":" + entry.y();
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(notifyPermission))
                .forEach(p -> p.sendMessage(message));
    }

    private void prune() {
        history.values().forEach(this::cleanupOld);
        long cutoff = Instant.now().minusSeconds(storeEventsDays * 86400L).toEpochMilli();
        suspicions.removeIf(entry -> entry.time() < cutoff);
        save();
    }

    public List<SuspiciousEntry> getRecent(int limit) {
        return suspicions.stream().limit(limit).collect(Collectors.toList());
    }

    public void markResolved(SuspiciousEntry entry) {
        entry.setResolved(true);
        save();
    }

    private void save() {
        if (file == null) return;
        FileConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (SuspiciousEntry entry : suspicions) {
            Map<String, Object> map = new HashMap<>();
            map.put("player", entry.player());
            map.put("material", entry.material());
            map.put("amount", entry.amount());
            map.put("time", entry.time());
            map.put("world", entry.world());
            map.put("y", entry.y());
            map.put("resolved", entry.resolved());
            list.add(map);
        }
        config.set("entries", list);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte suspicious-mining.yml nicht speichern: " + e.getMessage());
        }
    }

    public static String formatTime(long timestamp) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    public record MiningEvent(long timestamp, Material material, int y, String world) {
    }

    public static class SuspiciousEntry {
        private final String player;
        private final String material;
        private final int amount;
        private final long time;
        private final String world;
        private final int y;
        private boolean resolved;

        public SuspiciousEntry(String player, String material, int amount, long time, String world, int y, boolean resolved) {
            this.player = player;
            this.material = material;
            this.amount = amount;
            this.time = time;
            this.world = world;
            this.y = y;
            this.resolved = resolved;
        }

        public String player() {
            return player;
        }

        public String material() {
            return material;
        }

        public int amount() {
            return amount;
        }

        public long time() {
            return time;
        }

        public String world() {
            return world;
        }

        public int y() {
            return y;
        }

        public boolean resolved() {
            return resolved;
        }

        public void setResolved(boolean resolved) {
            this.resolved = resolved;
        }
    }
}
