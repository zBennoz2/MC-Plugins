package com.zbennoz.zbenclaims;

import java.util.Objects;
import java.util.UUID;

public final class ChunkKey {
    private final UUID world;
    private final int x;
    private final int z;

    public ChunkKey(UUID world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkKey other)) return false;
        return x == other.x && z == other.z && Objects.equals(world, other.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }
}
