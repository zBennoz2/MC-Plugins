package com.zbennoz.zbencoins.market;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Filter- und Sortieroptionen für den Marktplatz.
 */
public class MarketQueryOptions {

    public enum SortOption {
        PREIS_AUFSTEIGEND,
        PREIS_ABSTEIGEND,
        NEUESTE,
        ABLAUFEND
    }

    public enum Category {
        ALLES(item -> true),
        ERZE(item -> nameContains(item, "ORE") || nameContains(item, "INGOT") || nameContains(item, "RAW")),
        FARM(item -> nameContains(item, "WHEAT") || nameContains(item, "CARROT") || nameContains(item, "POTATO")
                || nameContains(item, "SEEDS") || nameContains(item, "BEEF") || nameContains(item, "PORKCHOP")),
        KAMPF(item -> nameContains(item, "SWORD") || nameContains(item, "BOW") || nameContains(item, "ARROW")
                || nameContains(item, "AXE") || nameContains(item, "HELMET") || nameContains(item, "CHESTPLATE")
                || nameContains(item, "LEGGINGS") || nameContains(item, "BOOTS")),
        BLOECKE(item -> item.getType().isBlock());

        private final Predicate<ItemStack> matcher;

        Category(Predicate<ItemStack> matcher) {
            this.matcher = matcher;
        }

        public boolean matches(ItemStack stack) {
            return matcher.test(stack);
        }

        private static boolean nameContains(ItemStack item, String key) {
            Material type = item.getType();
            return type.name().toUpperCase(Locale.ROOT).contains(key.toUpperCase(Locale.ROOT));
        }
    }

    private String searchTerm = "";
    private boolean onlineOnly = false;
    private Category category = Category.ALLES;
    private SortOption sortOption = SortOption.NEUESTE;
    private int page = 0;

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm == null ? "" : searchTerm.trim();
    }

    public boolean isOnlineOnly() {
        return onlineOnly;
    }

    public void setOnlineOnly(boolean onlineOnly) {
        this.onlineOnly = onlineOnly;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public SortOption getSortOption() {
        return sortOption;
    }

    public void setSortOption(SortOption sortOption) {
        this.sortOption = sortOption;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public void reset() {
        searchTerm = "";
        onlineOnly = false;
        category = Category.ALLES;
        sortOption = SortOption.NEUESTE;
        page = 0;
    }

    public MarketQueryOptions copy() {
        MarketQueryOptions options = new MarketQueryOptions();
        options.searchTerm = this.searchTerm;
        options.onlineOnly = this.onlineOnly;
        options.category = this.category;
        options.sortOption = this.sortOption;
        options.page = this.page;
        return options;
    }
}
