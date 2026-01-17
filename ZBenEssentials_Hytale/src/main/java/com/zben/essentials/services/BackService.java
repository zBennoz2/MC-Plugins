package com.zben.essentials.services;

import com.zben.essentials.model.PlayerLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BackService {
    private final Map<UUID, PlayerLocation> backLocations = new HashMap<>();

    public void setBackLocation(UUID playerId, PlayerLocation location) {
        if (location == null) {
            return;
        }
        backLocations.put(playerId, location);
    }

    public Optional<PlayerLocation> getBackLocation(UUID playerId) {
        return Optional.ofNullable(backLocations.get(playerId));
    }
}
