package com.zbennoz.zbencityjobs.commands;

import com.zbennoz.zbencityjobs.model.City;
import com.zbennoz.zbencityjobs.service.CityService;
import com.zbennoz.zbencityjobs.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class CityCommand implements CommandExecutor {
    private final CityService cityService;
    private final MessageService messages;
    private final double defaultTax;

    public CityCommand(CityService cityService, MessageService messages, double defaultTax) {
        this.cityService = cityService;
        this.messages = messages;
        this.defaultTax = defaultTax;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!sender.hasPermission("zbencityjobs.city.admin")) {
                    sender.sendMessage(messages.get("errors.no-permission"));
                    return true;
                }
                if (args.length < 2) return false;
                String name = args[1];
                Player actor = sender instanceof Player p ? p : null;
                cityService.createCity(actor, name, defaultTax).ifPresentOrElse(city ->
                        sender.sendMessage(messages.get("info.city-created", Map.of("name", city.getName()))),
                        () -> sender.sendMessage(messages.get("errors.transaction-failed")));
                return true;
            }
            case "mayor" -> {
                if (!sender.hasPermission("zbencityjobs.city.admin")) {
                    sender.sendMessage(messages.get("errors.no-permission"));
                    return true;
                }
                if (args.length < 3) return false;
                String cityName = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                Optional<City> city = cityService.findByName(cityName);
                city.ifPresent(value -> {
                    cityService.setMayor(value, target.getUniqueId());
                    sender.sendMessage(messages.get("info.mayor-set", Map.of("player", target.getName(), "city", value.getName())));
                });
                return true;
            }
            case "tax" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 3) return false;
                Optional<City> city = cityService.findByName(args[1]);
                if (city.isEmpty()) return true;
                City value = city.get();
                if (value.getMayor() == null || !value.getMayor().equals(player.getUniqueId())) {
                    player.sendMessage(messages.get("errors.no-permission"));
                    return true;
                }
                double percent = Double.parseDouble(args[2]);
                if (cityService.setTax(value, percent, player.getUniqueId())) {
                    player.sendMessage(messages.get("info.tax-set", Map.of("percent", String.valueOf(percent))));
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
