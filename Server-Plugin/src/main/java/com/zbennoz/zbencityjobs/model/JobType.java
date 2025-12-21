package com.zbennoz.zbencityjobs.model;

public enum JobType {
    DELIVERY,
    SERVICE;

    public static JobType fromString(String input) {
        if (input == null) return null;
        return switch (input.toLowerCase()) {
            case "delivery", "lieferung" -> DELIVERY;
            case "service", "dienst" -> SERVICE;
            default -> null;
        };
    }
}
