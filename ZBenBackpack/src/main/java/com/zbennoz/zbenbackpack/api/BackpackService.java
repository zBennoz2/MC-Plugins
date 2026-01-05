package com.zbennoz.zbenbackpack.api;

import com.zbennoz.zbenbackpack.ZBenBackpackPlugin;
import com.zbennoz.zbenbackpack.data.BackpackDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BackpackService {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(9, 18, 27, 36, 45, 54);

    private final ZBenBackpackPlugin plugin;
    private final BackpackDatabase database;
    private final NamespacedKey key;

    public BackpackService(ZBenBackpackPlugin plugin, BackpackDatabase database) {
        this.plugin = plugin;
        this.database = database;
        this.key = new NamespacedKey(plugin, "backpack");
    }

    public void openBackpack(Player player) {
        int size = resolveSize(player);
        BackpackState state = loadState(player.getUniqueId(), size);

        if (!state.overflow().isEmpty()) {
            giveOverflow(player, state.overflow());
        }

        Inventory inventory = Bukkit.createInventory(player, size, Component.text("Backpack (" + size + ")"));
        inventory.setContents(state.contents());
        database.saveBackpackAsync(player.getUniqueId().toString(), serialize(state.contents()), size);
        player.openInventory(inventory);
        player.sendMessage(Component.text("Backpack opened."));
    }

    public void saveBackpack(Player player) {
        Inventory open = player.getOpenInventory().getTopInventory();
        String data = serialize(open.getContents());
        database.saveBackpackAsync(player.getUniqueId().toString(), data, open.getSize());
    }

    public void applyBackpackSize(UUID playerId, int requestedSize) {
        int size = sanitizeSize(requestedSize);
        BackpackState state = loadState(playerId, size);

        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            if (!state.overflow().isEmpty()) {
                giveOverflow(online, state.overflow());
            }
            database.saveBackpackAsync(playerId.toString(), serialize(state.contents()), size);
            if (isViewingBackpack(online)) {
                openBackpack(online);
            }
        } else {
            database.saveBackpackAsync(playerId.toString(), serialize(state.storedItems()), size);
        }
    }

    public boolean isBackpackView(InventoryView view) {
        return view != null && Objects.equals(view.title(), Component.text("Backpack (" + view.getTopInventory().getSize() + ")"));
    }

    private BackpackState loadState(UUID playerId, int targetSize) {
        BackpackDatabase.BackpackRecord record = database.loadBackpack(playerId.toString());
        ItemStack[] stored = record == null ? new ItemStack[0] : deserialize(record.data());

        ItemStack[] target = new ItemStack[targetSize];
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < stored.length; i++) {
            if (i < targetSize) {
                target[i] = stored[i];
            } else {
                overflow.add(stored[i]);
            }
        }
        return new BackpackState(target, overflow, stored);
    }

    private int resolveSize(Player player) {
        BackpackDatabase.BackpackRecord record = database.loadBackpack(player.getUniqueId().toString());
        if (record != null && record.size() > 0) {
            return sanitizeSize(record.size());
        }
        int size = 9;
        for (int i : new int[]{54, 45, 36, 27, 18, 9}) {
            if (player.hasPermission("zbenbackpack.size." + i)) {
                size = i;
                break;
            }
        }
        return size;
    }

    private boolean isViewingBackpack(Player player) {
        return isBackpackView(player.getOpenInventory());
    }

    private void giveOverflow(Player player, List<ItemStack> overflow) {
        overflow.stream()
                .filter(Objects::nonNull)
                .forEach(item -> {
                    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                    leftovers.values().stream()
                            .filter(Objects::nonNull)
                            .forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
                });
    }

    public ItemStack createBackpackItem(Player player) {
        ItemStack stack = new ItemStack(Material.BUNDLE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Backpack"));
        meta.lore(java.util.List.of(Component.text("Right click to open")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isBackpack(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(key, PersistentDataType.BYTE);
    }

    private String serialize(ItemStack[] items) {
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
            config.set("i", items);
            outputStream.write(config.saveToString().getBytes());
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private ItemStack[] deserialize(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            String yaml = new String(bytes);
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
            config.loadFromString(yaml);
            List<ItemStack> items = (List<ItemStack>) config.getList("i", List.of());
            return items.toArray(new ItemStack[0]);
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }

    private int sanitizeSize(int size) {
        int normalized = size;
        if (!ALLOWED_SIZES.contains(size)) {
            normalized = 9;
        }
        return normalized;
    }

    public ZBenBackpackPlugin getPlugin() {
        return plugin;
    }

    private record BackpackState(ItemStack[] contents, List<ItemStack> overflow, ItemStack[] storedItems) {
    }
}
