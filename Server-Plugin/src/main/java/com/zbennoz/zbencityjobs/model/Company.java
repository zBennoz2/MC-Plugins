package com.zbennoz.zbencityjobs.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Company {
    private int id;
    private final String name;
    private final Map<UUID, CompanyRole> members = new HashMap<>();
    private UUID owner;

    public Company(int id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.members.put(owner, CompanyRole.OWNER);
    }

    public Company(String name, UUID owner) {
        this(0, name, owner);
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

    public Map<UUID, CompanyRole> getMembers() {
        return members;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        members.put(owner, CompanyRole.OWNER);
    }
}
