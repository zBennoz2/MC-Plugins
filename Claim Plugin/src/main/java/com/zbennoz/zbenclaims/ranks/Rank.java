package com.zbennoz.zbenclaims.ranks;

import java.util.Set;

public record Rank(String name, int priority, int limit, String tabPrefix, String chatPrefix, String nametagPrefix,
                   double cost, Set<String> flags, Set<String> permissions) { }
