package com.zbennoz.zbenadmintool.api;

import com.zbennoz.zbenadmintool.rank.Rank;
import com.zbennoz.zbenadmintool.rank.RankManager;

import java.util.UUID;

public class ZBenRankService implements ZBenRankAPI {

    private final RankManager rankManager;

    public ZBenRankService(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public String getRankName(UUID playerUuid) {
        Rank rank = rankManager.getPlayerRank(playerUuid);
        return rank != null ? rank.getName() : null;
    }

    @Override
    public int getMaxClaimChunks(UUID playerUuid) {
        Rank rank = rankManager.getPlayerRank(playerUuid);
        return rank != null ? rank.getMaxClaimChunks() : 0;
    }
}
