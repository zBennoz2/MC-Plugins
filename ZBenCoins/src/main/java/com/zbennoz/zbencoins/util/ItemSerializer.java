package com.zbennoz.zbencoins.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Serialisierung von ItemStacks inklusive Meta.
 */
public final class ItemSerializer {

    private ItemSerializer() {
    }

    public static String serialize(ItemStack itemStack) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(itemStack);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        }
    }

    public static ItemStack deserialize(String data) throws IOException {
        byte[] bytes = Base64Coder.decodeLines(data);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            Object object = dataInput.readObject();
            if (object instanceof ItemStack itemStack) {
                return itemStack;
            }
            throw new IOException("Ungültiges Item im Speicher");
        } catch (ClassNotFoundException e) {
            throw new IOException("Konnte Item nicht lesen", e);
        }
    }
}
