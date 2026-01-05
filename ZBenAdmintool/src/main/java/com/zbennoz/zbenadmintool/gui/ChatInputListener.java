package com.zbennoz.zbenadmintool.gui;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatInputListener implements Listener {

    private final ZBenAdmintool plugin;
    private final Map<UUID, RankCreateSession> sessions = new HashMap<>();

    public ChatInputListener(ZBenAdmintool plugin) {
        this.plugin = plugin;
    }

    public void startSession(Player player) {
        if (!plugin.getPermissionResolver().has(player, "zbenadmintool.rank.manage")) {
            player.sendMessage(plugin.getMessages().raw("no_permission"));
            return;
        }
        RankCreateSession session = new RankCreateSession();
        sessions.put(player.getUniqueId(), session);
        player.closeInventory();
        player.sendMessage(plugin.getMessages().raw("rank.create.prompt_name"));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        RankCreateSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> processMessage(player, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void processMessage(Player player, String message) {
        RankCreateSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (message.equalsIgnoreCase("abbrechen")) {
            sessions.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessages().raw("rank.create.cancelled"));
            return;
        }

        switch (session.getStep()) {
            case NAME -> handleNameStep(player, session, message);
            case COLOR -> handleColorStep(player, session, message);
            case PRIORITY -> handlePriorityStep(player, session, message);
            case BACKPACK -> handleBackpackStep(player, session, message);
        }
    }

    private void handleNameStep(Player player, RankCreateSession session, String message) {
        if (!plugin.getRankManager().isValidRankName(message)) {
            player.sendMessage(plugin.getMessages().raw("rank.invalid_name"));
            player.sendMessage(plugin.getMessages().raw("rank.create.prompt_name"));
            return;
        }
        if (plugin.getRankManager().getRank(message) != null) {
            player.sendMessage(plugin.getMessages().raw("rank.exists"));
            player.sendMessage(plugin.getMessages().raw("rank.create.prompt_name"));
            return;
        }
        session.setName(message);
        session.setStep(RankCreateSession.Step.COLOR);
        player.sendMessage(plugin.getMessages().raw("rank.create.prompt_color"));
    }

    private void handleColorStep(Player player, RankCreateSession session, String message) {
        if (message.equalsIgnoreCase("skip")) {
            session.setColor("white");
        } else if (plugin.getRankManager().isValidColor(message)) {
            session.setColor(message);
        } else {
            player.sendMessage(plugin.getMessages().raw("rank.invalid_color"));
            player.sendMessage(plugin.getMessages().raw("rank.create.prompt_color"));
            return;
        }
        session.setStep(RankCreateSession.Step.PRIORITY);
        player.sendMessage(plugin.getMessages().raw("rank.create.prompt_priority"));
    }

    private void handlePriorityStep(Player player, RankCreateSession session, String message) {
        int priority = 0;
        if (!message.equalsIgnoreCase("skip")) {
            try {
                priority = Integer.parseInt(message);
            } catch (NumberFormatException ex) {
                player.sendMessage(plugin.getMessages().raw("rank.invalid_priority"));
                player.sendMessage(plugin.getMessages().raw("rank.create.prompt_priority"));
                return;
            }
        }
        session.setPriority(priority);
        session.setStep(RankCreateSession.Step.BACKPACK);
        player.sendMessage(plugin.getMessages().raw("rank.create.prompt_backpack"));
    }

    private void handleBackpackStep(Player player, RankCreateSession session, String message) {
        int slots = plugin.getRankManager().defaultBackpackSlotsFor(session.getName());
        if (!message.equalsIgnoreCase("skip")) {
            try {
                slots = Integer.parseInt(message);
            } catch (NumberFormatException ex) {
                player.sendMessage(plugin.getMessages().raw("rank.invalid_backpack"));
                player.sendMessage(plugin.getMessages().raw("rank.create.prompt_backpack"));
                return;
            }
            if (!plugin.getRankManager().isValidBackpackSize(slots)) {
                player.sendMessage(plugin.getMessages().raw("rank.invalid_backpack"));
                player.sendMessage(plugin.getMessages().raw("rank.create.prompt_backpack"));
                return;
            }
        }
        session.setBackpackSlots(slots);
        finishCreation(player, session);
    }

    private void finishCreation(Player player, RankCreateSession session) {
        String legacyColor = plugin.getRankManager().parseLegacyColor(session.getColor());
        boolean success = plugin.getRankManager().createRank(
                session.getName(),
                session.getColor(),
                legacyColor,
                session.getPriority(),
                "",
                "",
                session.getBackpackSlots() == null ? plugin.getRankManager().defaultBackpackSlotsFor(session.getName()) : session.getBackpackSlots());
        if (success) {
            player.sendMessage(plugin.getMessages().raw("rank.created").replace("%name%", session.getName()));
            sessions.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> AdminMenuListener.openMenu(plugin, player), 1L);
        } else {
            player.sendMessage(plugin.getMessages().raw("rank.create_failed").replace("%name%", session.getName()));
            sessions.remove(player.getUniqueId());
        }
    }
}
