package com.zbennoz.zbenclaims;

import java.util.UUID;

public record Claim(long id, UUID ownerUuid, String worldName, UUID worldUuid, int chunkX, int chunkZ) { }
