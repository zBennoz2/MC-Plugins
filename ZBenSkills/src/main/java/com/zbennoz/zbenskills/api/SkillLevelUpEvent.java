package com.zbennoz.zbenskills.api;

import com.zbennoz.zbenskills.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final SkillType skill;
    private final int level;

    public SkillLevelUpEvent(Player player, SkillType skill, int level) {
        this.player = player;
        this.skill = skill;
        this.level = level;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
