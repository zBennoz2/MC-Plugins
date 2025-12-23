package com.zbennoz.zbenskills.config;

public class BenefitValue {
    private final double base;
    private final double perLevel;
    private final double max;

    public BenefitValue(double base, double perLevel, double max) {
        this.base = base;
        this.perLevel = perLevel;
        this.max = max;
    }

    public double calculate(int level, int prestige, double prestigeMultiplier) {
        int effectiveLevel = Math.max(1, level);
        double value = base + perLevel * Math.max(0, effectiveLevel - 1);
        value *= 1 + (Math.max(0, prestige) * prestigeMultiplier);
        return Math.min(max, value);
    }
}
