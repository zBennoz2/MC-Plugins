package com.zbennoz.zbencityjobs.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;

public class EconomyService {
    private Economy economy;

    public boolean setup() {
        if (Bukkit.getServer().getServicesManager().getRegistration(Economy.class) == null) {
            return false;
        }
        this.economy = Bukkit.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        return economy != null;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public boolean withdraw(UUID player, double amount) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        return economy.withdrawPlayer(offlinePlayer, amount).transactionSuccess();
    }

    public void deposit(UUID player, double amount) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        economy.depositPlayer(offlinePlayer, amount);
    }

    public double getBalance(UUID player) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        return economy.getBalance(offlinePlayer);
    }

    public Optional<Economy> getProvider() {
        return Optional.ofNullable(economy);
    }
}
