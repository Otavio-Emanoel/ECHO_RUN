package com.echorun.sprite;

import java.awt.Graphics2D;

public interface CharacterSprite {
    void paint(Graphics2D g2, int x, int y, int size, Direction dir, double timeSec);
}
