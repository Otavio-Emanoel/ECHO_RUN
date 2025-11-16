package com.echorun.game;

public enum PlayerClass {
    WARRIOR("Guerreiro", 120, 6.0, 15),
    MAGE("Mago", 80, 5.5, 25),
    ROGUE("Ladino", 90, 7.5, 12),
    RANGER("Arqueiro", 100, 7.0, 14),
    CLERIC("Clérigo", 110, 5.8, 10);

    private final String displayName;
    private final int baseHp;
    private final double moveSpeed; // pixels por frame em 60 FPS aprox
    private final int attackDamage;

    PlayerClass(String displayName, int baseHp, double moveSpeed, int attackDamage) {
        this.displayName = displayName;
        this.baseHp = baseHp;
        this.moveSpeed = moveSpeed;
        this.attackDamage = attackDamage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public int getAttackDamage() {
        return attackDamage;
    }
}
