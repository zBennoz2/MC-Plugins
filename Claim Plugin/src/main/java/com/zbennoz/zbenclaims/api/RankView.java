package com.zbennoz.zbenclaims.api;

/**
 * Simple immutable view of a player's rank used by other plugins.
 */
public record RankView(String key, String tabPrefix, String chatPrefix, String nameTagPrefix, int claimLimit) {
}
