package com.zbennoz.zbencoins.gui;

import com.zbennoz.zbencoins.util.Text;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Bequemer Builder für GUI Items.
 */
public class GuiItemBuilder {

    private final ItemStack stack;

    public GuiItemBuilder(Material material) {
        this.stack = new ItemStack(material);
    }

    public GuiItemBuilder name(String name) {
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Text.colorize(name));
        stack.setItemMeta(meta);
        return this;
    }

    public GuiItemBuilder lore(List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(Text.colorize(line));
        }
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return this;
    }

    public GuiItemBuilder hideFlags() {
        ItemMeta meta = stack.getItemMeta();
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return stack;
    }
}
