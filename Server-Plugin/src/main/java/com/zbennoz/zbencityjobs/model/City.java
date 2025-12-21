package com.zbennoz.zbencityjobs.model;

import java.util.UUID;

public class City {
    private int id;
    private final String name;
    private UUID mayor;
    private double taxPercent;

    public City(int id, String name, UUID mayor, double taxPercent) {
        this.id = id;
        this.name = name;
        this.mayor = mayor;
        this.taxPercent = taxPercent;
    }

    public City(String name, UUID mayor, double taxPercent) {
        this(0, name, mayor, taxPercent);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public UUID getMayor() {
        return mayor;
    }

    public void setMayor(UUID mayor) {
        this.mayor = mayor;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(double taxPercent) {
        this.taxPercent = taxPercent;
    }
}
