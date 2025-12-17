package com.zbennoz.zbenclaims;

import java.util.UUID;

public record Claim(long id, UUID ownerUuid, String world, int chunkX, int chunkZ) { }
