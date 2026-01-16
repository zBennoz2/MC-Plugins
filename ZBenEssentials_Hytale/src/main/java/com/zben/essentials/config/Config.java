package com.zben.essentials.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private String language = "de_DE";
    private String prefix = "[ZBen]";
    private WelcomeConfig welcome = new WelcomeConfig();
    private JoinQuitConfig joinQuit = new JoinQuitConfig();
    private Map<String, ChatFormatConfig> chatFormats = new HashMap<>();
    private Map<String, GroupConfig> groups = new HashMap<>();
    private String defaultGroup = "default";
    private Map<String, String> messageOverrides = new HashMap<>();

    public Config() {
        chatFormats.put("default", new ChatFormatConfig("{prefix} {player}: {message}", 1));

        GroupConfig defaultGroupConfig = new GroupConfig();
        defaultGroupConfig.getPermissions().add("zben.ping");
        defaultGroupConfig.getPermissions().add("zben.whoami");
        groups.put("default", defaultGroupConfig);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public WelcomeConfig getWelcome() {
        return welcome;
    }

    public void setWelcome(WelcomeConfig welcome) {
        this.welcome = welcome;
    }

    public JoinQuitConfig getJoinQuit() {
        return joinQuit;
    }

    public void setJoinQuit(JoinQuitConfig joinQuit) {
        this.joinQuit = joinQuit;
    }

    public Map<String, ChatFormatConfig> getChatFormats() {
        return chatFormats;
    }

    public void setChatFormats(Map<String, ChatFormatConfig> chatFormats) {
        this.chatFormats = chatFormats;
    }

    public Map<String, GroupConfig> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, GroupConfig> groups) {
        this.groups = groups;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public Map<String, String> getMessageOverrides() {
        return messageOverrides;
    }

    public void setMessageOverrides(Map<String, String> messageOverrides) {
        this.messageOverrides = messageOverrides;
    }

    public static class WelcomeConfig {
        private boolean enabled = true;
        private boolean broadcastToAll = true;
        private List<String> messages = new ArrayList<>(List.of("Willkommen {player}!"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBroadcastToAll() {
            return broadcastToAll;
        }

        public void setBroadcastToAll(boolean broadcastToAll) {
            this.broadcastToAll = broadcastToAll;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }
    }

    public static class JoinQuitConfig {
        private boolean enabled = true;
        private String joinMessage = "{player} hat den Server betreten.";
        private String quitMessage = "{player} hat den Server verlassen.";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJoinMessage() {
            return joinMessage;
        }

        public void setJoinMessage(String joinMessage) {
            this.joinMessage = joinMessage;
        }

        public String getQuitMessage() {
            return quitMessage;
        }

        public void setQuitMessage(String quitMessage) {
            this.quitMessage = quitMessage;
        }
    }

    public static class ChatFormatConfig {
        private String format;
        private int priority;

        public ChatFormatConfig() {
        }

        public ChatFormatConfig(String format, int priority) {
            this.format = format;
            this.priority = priority;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    public static class GroupConfig {
        private List<String> permissions = new ArrayList<>();

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }
}
