package com.zbennoz.zbenadmintool.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

public class BackpackIntegration {

    private final JavaPlugin plugin;
    private final Logger logger;
    private Object targetInstance;
    private Method setSizeMethod;

    public BackpackIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        detect();
    }

    private void detect() {
        Plugin backpack = Bukkit.getPluginManager().getPlugin("ZBenBackpack");
        if (backpack == null || !backpack.isEnabled()) {
            return;
        }
        targetInstance = backpack;
        setSizeMethod = findSetter(backpack.getClass());

        if (setSizeMethod == null) {
            Object manager = invokeIfPresent(backpack, "getBackpackManager");
            if (manager != null) {
                setSizeMethod = findSetter(manager.getClass());
                if (setSizeMethod != null) {
                    targetInstance = manager;
                }
            }
        }

        if (setSizeMethod != null) {
            logger.info("ZBenBackpack erkannt – Backpack-Slots werden bei Rangwechseln synchronisiert.");
        }
    }

    private Object invokeIfPresent(Object instance, String method) {
        try {
            Method m = instance.getClass().getMethod(method);
            return m.invoke(instance);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.warning("Konnte Backpack-Integration nicht initialisieren: " + e.getMessage());
            return null;
        }
    }

    private Method findSetter(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.getName().equalsIgnoreCase("setBackpackSize")
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0] == UUID.class
                    && method.getParameterTypes()[1] == int.class) {
                return method;
            }
        }
        return null;
    }

    public boolean isAvailable() {
        return targetInstance != null && setSizeMethod != null;
    }

    public void applyBackpackSize(UUID uuid, int slots) {
        if (!isAvailable()) {
            return;
        }
        try {
            setSizeMethod.invoke(targetInstance, uuid, slots);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.warning("Backpack-Slots konnten nicht synchronisiert werden: " + e.getMessage());
        }
    }
}
