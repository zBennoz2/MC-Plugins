package com.zbennoz.zbenskills.api;

import com.zbennoz.zbenskills.model.SkillNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NodeUnlockEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final SkillNode node;

    public NodeUnlockEvent(Player player, SkillNode node) {
        this.player = player;
        this.node = node;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillNode getNode() {
        return node;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
