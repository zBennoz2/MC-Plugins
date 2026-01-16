package com.zben.essentials.services;

import com.zben.essentials.config.Config;
import com.zben.essentials.util.WildcardMatcher;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PermissionService {
    private final ConfigService configService;
    private final UserService userService;

    public PermissionService(ConfigService configService, UserService userService) {
        this.configService = configService;
        this.userService = userService;
    }

    public String resolveGroup(UUID playerId) {
        String storedGroup = userService.getGroup(playerId);
        if (storedGroup != null && configService.getConfig().getGroups().containsKey(storedGroup)) {
            return storedGroup;
        }
        return configService.getConfig().getDefaultGroup();
    }

    public boolean hasPermission(UUID playerId, String permission) {
        Config config = configService.getConfig();
        String group = resolveGroup(playerId);
        Map<String, Config.GroupConfig> groups = config.getGroups();
        Config.GroupConfig groupConfig = groups.get(group);
        if (groupConfig == null) {
            return false;
        }

        List<String> permissions = groupConfig.getPermissions();
        return WildcardMatcher.matches(permission, permissions);
    }
}
