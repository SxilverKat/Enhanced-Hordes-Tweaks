package com.enhancedhordes.tweaks.config;

public enum DifficultyPreset {

    EASY(0.5, 2.0, 0.5, 0.5),
    NORMAL(1.0, 1.0, 1.0, 1.0),
    HARD(1.5, 0.5, 1.5, 1.5),
    NIGHTMARE(2.0, 0.0, 2.0, 2.0);

    public final double rangeMultiplier;
    public final double daysMultiplier;
    public final double increaseMultiplier;
    public final double damageMultiplier;

    DifficultyPreset(double rangeMultiplier, double daysMultiplier, double increaseMultiplier, double damageMultiplier) {
        this.rangeMultiplier = rangeMultiplier;
        this.daysMultiplier = daysMultiplier;
        this.increaseMultiplier = increaseMultiplier;
        this.damageMultiplier = damageMultiplier;
    }

    public boolean modifiesValues() {
        return this != NORMAL;
    }
}
