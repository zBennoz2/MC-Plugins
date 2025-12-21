package com.zbennoz.zbenskills.api;

import com.zbennoz.zbenskills.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrestigeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final SkillType skill;
    private final int prestigeLevel;

    public PrestigeEvent(Player player, SkillType skill, int prestigeLevel) {
        this.player = player;
        this.skill = skill;
        this.prestigeLevel = prestigeLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getPrestigeLevel() {
        return prestigeLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
