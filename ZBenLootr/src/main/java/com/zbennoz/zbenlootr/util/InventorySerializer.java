package com.zbennoz.zbenlootr.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class InventorySerializer {

    private InventorySerializer() {}

    public static String toBase64(Inventory inventory) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(inventory.getSize());
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize inventory", e);
        }
    }

    public static Inventory fromBase64(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            int size = dataInput.readInt();
            Inventory inventory = Bukkit.createInventory(null, size);
            for (int i = 0; i < size; i++) {
                ItemStack item = (ItemStack) dataInput.readObject();
                inventory.setItem(i, item);
            }
            return inventory;
        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalStateException("Unable to deserialize inventory", e);
        }
    }
}
