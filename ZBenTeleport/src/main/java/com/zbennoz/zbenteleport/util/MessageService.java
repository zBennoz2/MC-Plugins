package com.zbennoz.zbenteleport.util;

import com.zbennoz.zbenteleport.ZBenTeleportPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageService {

    private final ZBenTeleportPlugin plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration messages;

    public MessageService(ZBenTeleportPlugin plugin, MiniMessage miniMessage) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public Component component(String path, TagResolver... resolvers) {
        String raw = messages.getString(path, "<red>Missing message: " + path + "</red>");
        TagResolver resolver = TagResolver.resolver(resolvers);
        return miniMessage.deserialize(raw, resolver);
    }

    public Component component(String path, String placeholder, Component value) {
        return component(path, Placeholder.component(placeholder, value));
    }
}
