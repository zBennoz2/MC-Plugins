package com.zbennoz.zbencityjobs.model;

public enum CompanyRole {
    OWNER,
    MANAGER,
    MEMBER;

    public static CompanyRole fromString(String input) {
        if (input == null) return null;
        return switch (input.toLowerCase()) {
            case "owner" -> OWNER;
            case "manager" -> MANAGER;
            case "member" -> MEMBER;
            default -> null;
        };
    }
}
