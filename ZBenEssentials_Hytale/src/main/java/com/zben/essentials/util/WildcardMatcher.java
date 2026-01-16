package com.zben.essentials.util;

import java.util.List;

public final class WildcardMatcher {
    private WildcardMatcher() {
    }

    public static boolean matches(String permission, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (matches(permission, candidate)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(String permission, String candidate) {
        if (candidate == null) {
            return false;
        }
        if (candidate.equals("*")) {
            return true;
        }
        if (candidate.endsWith(".*")) {
            String prefix = candidate.substring(0, candidate.length() - 2);
            return permission.startsWith(prefix + ".");
        }
        return candidate.equalsIgnoreCase(permission);
    }
}
