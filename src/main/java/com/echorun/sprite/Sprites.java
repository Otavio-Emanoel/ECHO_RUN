package com.echorun.sprite;

import com.echorun.game.PlayerClass;

import java.awt.*;

public class Sprites {
    public static CharacterSprite forClass(PlayerClass c) {
        switch (c) {
            case WARRIOR: return Sprites::paintWarrior;
            case MAGE: return Sprites::paintMage;
            case ROGUE: return Sprites::paintRogue;
            case RANGER: return Sprites::paintRanger;
            case CLERIC: return Sprites::paintCleric;
            default: return Sprites::paintWarrior;
        }
    }

    // Util
    private static int u(int size, double v) { return (int)Math.round(v * size / 18.0); }
    private static void body(Graphics2D g2, int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.fillRoundRect(x + u(size,3), y + u(size,6), u(size,12), u(size,10), u(size,4), u(size,4));
    }

    private static void head(Graphics2D g2, int x, int y, int size, Color skin) {
        g2.setColor(skin);
        g2.fillRoundRect(x + u(size,4), y + u(size,1), u(size,10), u(size,10), u(size,6), u(size,6));
    }

    private static void eyes(Graphics2D g2, int x, int y, int size, Direction dir) {
        g2.setColor(new Color(30,30,35));
        int ex = x + u(size,7);
        int ey = y + u(size,6);
        if (dir == Direction.LEFT) ex -= u(size,1);
        if (dir == Direction.RIGHT) ex += u(size,1);
        g2.fillRect(ex, ey, u(size,2), u(size,2));
        g2.fillRect(ex + u(size,4), ey, u(size,2), u(size,2));
    }

    private static void shadowFeet(Graphics2D g2, int x, int y, int size) {
        g2.setColor(new Color(0,0,0,50));
        g2.fillOval(x + u(size,3), y + u(size,16), u(size,12), u(size,3));
    }

    private static int bobY(double timeSec) {
        return (int)Math.round(Math.sin(timeSec * Math.PI * 2.0) * 1.5);
    }

    private static void weaponSword(Graphics2D g2, int x, int y, int size, Direction dir) {
        g2.setColor(new Color(180,180,190));
        int wx = x + (dir == Direction.LEFT ? u(size,2) : u(size,14));
        int wy = y + u(size,10);
        int w = u(size,2), h = u(size,8);
        g2.fillRoundRect(wx - (dir==Direction.LEFT? w:0), wy, w, h, u(size,2), u(size,2));
        g2.setColor(new Color(130,90,60));
        g2.fillRect(wx - (dir==Direction.LEFT? w:0), wy + h, w, u(size,3));
    }

    private static void staff(Graphics2D g2, int x, int y, int size, Direction dir, Color tip) {
        g2.setColor(new Color(120, 92, 60));
        int wx = x + (dir == Direction.LEFT ? u(size,2) : u(size,15));
        int wy = y + u(size,6);
        g2.fillRect(wx - (dir==Direction.LEFT? u(size,1):0), wy, u(size,2), u(size,12));
        g2.setColor(tip);
        g2.fillOval(wx - u(size,1), wy - u(size,2), u(size,4), u(size,4));
    }

    // Classes
    private static void paintWarrior(Graphics2D g2, int x, int y, int size, Direction dir, double t) {
        int bob = bobY(t);
        shadowFeet(g2, x, y, size);
        // capa
        g2.setColor(new Color(160,30,40));
        g2.fillRoundRect(x + u(size,3), y + u(size,8)+bob, u(size,12), u(size,8), u(size,4), u(size,4));
        // corpo
        body(g2, x, y + bob, size, new Color(90, 100, 120));
        // cabeça + elmo
        head(g2, x, y + bob, size, new Color(235, 210, 190));
        g2.setColor(new Color(130, 140, 160));
        g2.fillRoundRect(x + u(size,4), y + u(size,0)+bob, u(size,10), u(size,6), u(size,4), u(size,4));
        eyes(g2, x, y + bob, size, dir);
        weaponSword(g2, x, y + bob, size, dir);
    }

    private static void paintMage(Graphics2D g2, int x, int y, int size, Direction dir, double t) {
        int bob = bobY(t);
        shadowFeet(g2, x, y, size);
        // manto
        body(g2, x, y + bob, size, new Color(60, 90, 160));
        // gola
        g2.setColor(new Color(40,60,110));
        g2.fillRoundRect(x + u(size,4), y + u(size,8)+bob, u(size,10), u(size,4), u(size,3), u(size,3));
        // cabeça + chapéu
        head(g2, x, y + bob, size, new Color(240, 220, 200));
        g2.setColor(new Color(50, 70, 130));
        g2.fillPolygon(new int[]{x+u(size,5), x+u(size,9), x+u(size,13)}, new int[]{y+u(size,6)+bob, y+u(size,0)+bob, y+u(size,6)+bob}, 3);
        eyes(g2, x, y + bob, size, dir);
        staff(g2, x, y + bob, size, dir, new Color(140, 200, 255));
    }

    private static void paintRogue(Graphics2D g2, int x, int y, int size, Direction dir, double t) {
        int bob = bobY(t);
        shadowFeet(g2, x, y, size);
        // capuz
        g2.setColor(new Color(34, 58, 60));
        g2.fillRoundRect(x + u(size,3), y + u(size,2)+bob, u(size,12), u(size,8), u(size,5), u(size,5));
        // corpo escuro
        body(g2, x, y + bob, size, new Color(40, 60, 64));
        // faixa
        g2.setColor(new Color(80,120,100));
        g2.fillRect(x + u(size,4), y + u(size,12)+bob, u(size,10), u(size,2));
        // máscara + olhos
        g2.setColor(new Color(20,30,32));
        g2.fillRoundRect(x + u(size,5), y + u(size,5)+bob, u(size,8), u(size,4), u(size,3), u(size,3));
        eyes(g2, x, y + bob, size, dir);
    }

    private static void paintRanger(Graphics2D g2, int x, int y, int size, Direction dir, double t) {
        int bob = bobY(t);
        shadowFeet(g2, x, y, size);
        // capuz verde
        g2.setColor(new Color(52, 94, 60));
        g2.fillRoundRect(x + u(size,3), y + u(size,2)+bob, u(size,12), u(size,8), u(size,5), u(size,5));
        // corpo couro
        body(g2, x, y + bob, size, new Color(110, 84, 60));
        // aljava
        g2.setColor(new Color(92, 64, 44));
        g2.fillRect(x + u(size,2), y + u(size,6)+bob, u(size,3), u(size,8));
        // arco
        g2.setColor(new Color(120, 90, 60));
        int ax = x + (dir == Direction.LEFT ? u(size,3) : u(size,15));
        g2.drawArc(ax - u(size,1), y + u(size,6)+bob, u(size,4), u(size,8), 90, 180);
        eyes(g2, x, y + bob, size, dir);
    }

    private static void paintCleric(Graphics2D g2, int x, int y, int size, Direction dir, double t) {
        int bob = bobY(t);
        shadowFeet(g2, x, y, size);
        // túnica branca
        body(g2, x, y + bob, size, new Color(220, 220, 230));
        // detalhes dourados
        g2.setColor(new Color(200, 170, 60));
        g2.fillRect(x + u(size,8), y + u(size,8)+bob, u(size,2), u(size,8));
        // cabeça
        head(g2, x, y + bob, size, new Color(245, 225, 205));
        eyes(g2, x, y + bob, size, dir);
        // cajado cruz
        staff(g2, x, y + bob, size, dir, new Color(255, 220, 120));
    }
}
