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

    public RequestOutcome acceptRequest(UUID targetId, Duration timeout) {
        return consumeRequest(targetId, timeout);
    }

    public RequestOutcome denyRequest(UUID targetId, Duration timeout) {
        return consumeRequest(targetId, timeout);
    }

    public RequestOutcome consumeRequest(UUID targetId, Duration timeout) {
        Instant now = Instant.now();
        TpaRequest request = requestsByTarget.get(targetId);
        if (request == null) {
            expireOldRequests(timeout, now);
            return new RequestOutcome(RequestStatus.NOT_FOUND, null);
        }
        if (isExpired(request, timeout, now)) {
            removeRequest(targetId);
            expireOldRequests(timeout, now);
            return new RequestOutcome(RequestStatus.EXPIRED, null);
        }
        Optional<TpaRequest> resolved = removeRequest(targetId);
        expireOldRequests(timeout, now);
        return new RequestOutcome(RequestStatus.FOUND, resolved.orElse(null));
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
        expireOldRequests(timeout, now);
    }

    private void expireOldRequests(Duration timeout, Instant now) {
        if (timeout.isZero() || timeout.isNegative()) {
            return;
        }
        Iterator<Map.Entry<UUID, TpaRequest>> iterator = requestsBySender.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TpaRequest> entry = iterator.next();
            TpaRequest request = entry.getValue();
            if (isExpired(request, timeout, now)) {
                iterator.remove();
                requestsByTarget.remove(request.getTargetId());
            }
        }
    }

    private boolean isExpired(TpaRequest request, Duration timeout) {
        return isExpired(request, timeout, Instant.now());
    }

    private boolean isExpired(TpaRequest request, Duration timeout, Instant now) {
        if (timeout.isZero() || timeout.isNegative()) {
            return false;
        }
        return Duration.between(request.getCreatedAt(), now).compareTo(timeout) > 0;
    }

    public enum RequestStatus {
        FOUND,
        EXPIRED,
        NOT_FOUND
    }

    public static class RequestOutcome {
        private final RequestStatus status;
        private final TpaRequest request;

        public RequestOutcome(RequestStatus status, TpaRequest request) {
            this.status = status;
            this.request = request;
        }

        public RequestStatus getStatus() {
            return status;
        }

        public Optional<TpaRequest> getRequest() {
            return Optional.ofNullable(request);
        }
    }
}
