package com.zben.essentials.services;

import com.zben.essentials.model.TpaRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TpaService {
    private final Map<UUID, TpaRequest> requestsBySender = new HashMap<>();
    private final Map<UUID, TpaRequest> requestsByTarget = new HashMap<>();
    private final Map<UUID, Instant> lastRequestTimes = new HashMap<>();

    public Optional<Duration> getCooldownRemaining(UUID senderId, Duration cooldown) {
        Instant lastRequest = lastRequestTimes.get(senderId);
        if (lastRequest == null) {
            return Optional.empty();
        }
        Duration elapsed = Duration.between(lastRequest, Instant.now());
        if (elapsed.compareTo(cooldown) >= 0) {
            return Optional.empty();
        }
        return Optional.of(cooldown.minus(elapsed));
    }

    public boolean hasActiveRequest(UUID playerId, Duration timeout) {
        expireOldRequests(timeout);
        return requestsBySender.containsKey(playerId) || requestsByTarget.containsKey(playerId);
    }

    public Optional<TpaRequest> getRequestForTarget(UUID targetId, Duration timeout) {
        expireOldRequests(timeout);
        return Optional.ofNullable(requestsByTarget.get(targetId));
    }

    public TpaRequest createRequest(UUID senderId,
                                    String senderName,
                                    UUID targetId,
                                    String targetName,
                                    boolean teleportHere) {
        TpaRequest request = new TpaRequest(senderId, senderName, targetId, targetName, teleportHere, Instant.now());
        requestsBySender.put(senderId, request);
        requestsByTarget.put(targetId, request);
        lastRequestTimes.put(senderId, Instant.now());
        return request;
    }

    public Optional<TpaRequest> acceptRequest(UUID targetId, Duration timeout) {
        expireOldRequests(timeout);
        return removeRequest(targetId);
    }

    public Optional<TpaRequest> denyRequest(UUID targetId, Duration timeout) {
        expireOldRequests(timeout);
        return removeRequest(targetId);
    }

    private Optional<TpaRequest> removeRequest(UUID targetId) {
        TpaRequest request = requestsByTarget.remove(targetId);
        if (request != null) {
            requestsBySender.remove(request.getSenderId());
        }
        return Optional.ofNullable(request);
    }

    private void expireOldRequests(Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) {
            return;
        }
        Instant now = Instant.now();
        Iterator<Map.Entry<UUID, TpaRequest>> iterator = requestsBySender.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TpaRequest> entry = iterator.next();
            TpaRequest request = entry.getValue();
            if (Duration.between(request.getCreatedAt(), now).compareTo(timeout) > 0) {
                iterator.remove();
                requestsByTarget.remove(request.getTargetId());
            }
        }
    }
}
