package com.zbennoz.zbenteleport.util;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {

    private record Request(UUID sender, long expiresAt) {}

    private final Map<UUID, Request> incoming = new HashMap<>();
    private final ZBenTeleportPlugin plugin;

    public TpaManager(ZBenTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target) {
        long expires = Instant.now().plusSeconds(plugin.getConfig().getLong("tpa.expire-seconds", 60)).toEpochMilli();
        incoming.put(target.getUniqueId(), new Request(sender.getUniqueId(), expires));
    }

    public UUID consume(Player target, boolean accept) {
        Request request = incoming.get(target.getUniqueId());
        if (request == null) {
            return null;
        }
        if (request.expiresAt < Instant.now().toEpochMilli()) {
            incoming.remove(target.getUniqueId());
            return null;
        }
        incoming.remove(target.getUniqueId());
        return accept ? request.sender : null;
    }

    public void cancel(Player sender) {
        incoming.entrySet().removeIf(entry -> entry.getValue().sender.equals(sender.getUniqueId()));
    }
}
