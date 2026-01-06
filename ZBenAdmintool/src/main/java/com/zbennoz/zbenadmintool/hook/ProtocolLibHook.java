package com.zbennoz.zbenadmintool.hook;

import com.zbennoz.zbenadmintool.ZBenAdmintool;
import org.bukkit.Bukkit;

public class ProtocolLibHook {

    private final ZBenAdmintool plugin;
    private boolean available;

    public ProtocolLibHook(ZBenAdmintool plugin) {
        this.plugin = plugin;
        detect();
    }

    private void detect() {
        available = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
        if (available) {
            plugin.getLogger().info("ProtocolLib erkannt – Ore-Xray View aktiviert.");
        } else {
            plugin.getLogger().info("ProtocolLib nicht gefunden – Ore-Xray View bleibt deaktiviert.");
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
