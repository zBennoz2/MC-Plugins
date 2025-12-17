package com.zbennoz.zbenclaims;

import java.util.Objects;

public final class ChunkKey {
    private final String world;
    private final int x;
    private final int z;

    public ChunkKey(String world, int x, int z) {
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
