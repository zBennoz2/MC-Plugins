package com.zbennoz.zbencore.branding;

import com.zbennoz.zbencore.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BrandingService {

    // Branding is hardcoded by design:
    public static final String HOST_LINE = "&7Server hosted by &aZBenNoZ Gaming";
    public static final String WEB_LINE  = "&ahttps://zbennoz.com";

    private final JavaPlugin plugin;

    public BrandingService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyTablist(Player player) {
        if (!plugin.getConfig().getBoolean("features.tablistBranding", true)) return;
        player.setPlayerListHeaderFooter(Msg.color(HOST_LINE), Msg.color(WEB_LINE));
    }

    public String aboutLine1() {
        return Msg.color("&7Server hosted by &aZBenNoZ Gaming");
    }

    public String aboutLine2() {
        return Msg.color("&aWebsite: &7https://zbennoz.com");
    }

    public String joinHint() {
        return Msg.color(HOST_LINE + " &8|&r " + WEB_LINE);
    }
}
