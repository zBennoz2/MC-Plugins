package com.zben.essentials.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InteractiveMessage {
    private final String text;
    private final List<Action> actions;

    public InteractiveMessage(String text, List<Action> actions) {
        this.text = text;
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }

    public String getText() {
        return text;
    }

    public List<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public static class Action {
        private final String label;
        private final String command;
        private final String hoverText;

        public Action(String label, String command, String hoverText) {
            this.label = label;
            this.command = command;
            this.hoverText = hoverText;
        }

        public String getLabel() {
            return label;
        }

        public String getCommand() {
            return command;
        }

        public String getHoverText() {
            return hoverText;
        }
    }
}
