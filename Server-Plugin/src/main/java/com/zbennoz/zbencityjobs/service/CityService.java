package com.zbennoz.zbencityjobs.service;

import com.zbennoz.zbencityjobs.model.City;
import com.zbennoz.zbencityjobs.repository.CityRepository;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class CityService {
    private final CityRepository repository;
    private final AuditService auditService;

    public CityService(CityRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public Optional<City> createCity(Player actor, String name, double defaultTax) {
        City city = new City(name, actor != null ? actor.getUniqueId() : null, defaultTax);
        try {
            int id = repository.insert(city);
            city.setId(id);
            auditService.log(actor != null ? actor.getUniqueId() : null, "city.create", "city=" + name);
            return Optional.of(city);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public Optional<City> findByName(String name) {
        try {
            return repository.findByName(name);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public boolean setMayor(City city, UUID mayor) {
        city.setMayor(mayor);
        try {
            repository.update(city);
            auditService.log(mayor, "city.mayor", "city=" + city.getName());
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean setTax(City city, double percent, UUID actor) {
        city.setTaxPercent(percent);
        try {
            repository.update(city);
            auditService.log(actor, "city.tax", "city=" + city.getName() + ",tax=" + percent);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
