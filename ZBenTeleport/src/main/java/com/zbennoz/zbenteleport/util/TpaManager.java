package com.zbennoz.zbenteleport.util;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TpaManager {

    public enum RequestType {
        TPA,
        TPA_HERE
    }

    public static class Request {
        private final UUID id;
        private final UUID sender;
        private final UUID target;
        private final long expiresAt;
        private final RequestType type;

        public Request(UUID id, UUID sender, UUID target, long expiresAt, RequestType type) {
            this.id = id;
            this.sender = sender;
            this.target = target;
            this.expiresAt = expiresAt;
            this.type = type;
        }

        public UUID id() {
            return id;
        }

        public UUID sender() {
            return sender;
        }

        public UUID target() {
            return target;
        }

        public long expiresAt() {
            return expiresAt;
        }

        public RequestType type() {
            return type;
        }
    }

    public enum RequestResult {
        CREATED,
        REPLACED,
        DUPLICATE
    }

    private final Map<UUID, Request> incoming = new HashMap<>();
    private final Map<UUID, Request> outgoing = new HashMap<>();
    private final ZBenTeleportPlugin plugin;

    public TpaManager(ZBenTeleportPlugin plugin) {
        this.plugin = plugin;
    }

    public RequestStatus createRequest(Player sender, Player target, RequestType type) {
        long expires = Instant.now().plusSeconds(plugin.getConfig().getLong("tpa.timeoutSeconds", 60)).toEpochMilli();
        boolean replace = plugin.getConfig().getBoolean("tpa.replaceExisting", true);

        Request existingOutgoing = outgoing.get(sender.getUniqueId());
        if (existingOutgoing != null) {
            if (!replace) {
                return new RequestStatus(RequestResult.DUPLICATE, existingOutgoing);
            }
            cancel(existingOutgoing);
        }

        Request request = new Request(randomId(), sender.getUniqueId(), target.getUniqueId(), expires, type);
        incoming.put(target.getUniqueId(), request);
        outgoing.put(sender.getUniqueId(), request);
        return new RequestStatus(existingOutgoing != null ? RequestResult.REPLACED : RequestResult.CREATED, request);
    }

    public Request consume(Player target, UUID requestId) {
        Request request = incoming.get(target.getUniqueId());
        if (request == null) {
            return null;
        }
        if (requestId != null && !request.id.equals(requestId)) {
            return null;
        }
        if (request.expiresAt < Instant.now().toEpochMilli()) {
            cancel(request);
            return null;
        }
        incoming.remove(target.getUniqueId());
        outgoing.remove(request.sender);
        return request;
    }

    public boolean cancelOutgoing(Player sender) {
        Request request = outgoing.remove(sender.getUniqueId());
        if (request == null) {
            return false;
        }
        incoming.remove(request.target);
        return true;
    }

    public void expireRequests() {
        long now = Instant.now().toEpochMilli();
        List<Request> expired = new ArrayList<>();
        for (Request request : incoming.values()) {
            if (request.expiresAt < now) {
                expired.add(request);
            }
        }
        for (Request request : expired) {
            cancel(request);
            Player sender = Bukkit.getPlayer(request.sender);
            if (sender != null) {
                sender.sendMessage(plugin.messages().component("tpa.expired.sender"));
            }
            Player target = Bukkit.getPlayer(request.target);
            if (target != null) {
                target.sendMessage(plugin.messages().component("tpa.expired.target"));
            }
        }
    }

    private void cancel(Request request) {
        incoming.remove(request.target);
        outgoing.remove(request.sender);
    }

    private UUID randomId() {
        return new UUID(ThreadLocalRandom.current().nextLong(), ThreadLocalRandom.current().nextLong());
    }

    public record RequestStatus(RequestResult result, Request request) {
    }
}
