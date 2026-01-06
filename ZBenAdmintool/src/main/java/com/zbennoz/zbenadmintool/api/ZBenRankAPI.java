package com.zbennoz.zbenadmintool.api;

import java.util.UUID;

public interface ZBenRankAPI {
    String getRankName(UUID playerUuid);

    int getMaxClaimChunks(UUID playerUuid);
}
