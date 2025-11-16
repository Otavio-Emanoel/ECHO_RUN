package com.echorun.game;

import com.echorun.map.DungeonGenerator;
import com.echorun.map.DungeonMap;
import com.echorun.map.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import com.echorun.sprite.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GamePanel extends JPanel implements Runnable {
    private final PlayerClass playerClass;
    private final Runnable onExitToMenu;

    private Thread loopThread;
    private volatile boolean running = false;

    // Mundo / mapa
    private DungeonMap map;
    private final int tileSize = 24;

    // Jogador
    private double playerX = 100;
    private double playerY = 100;
    private double velX = 0;
    private double velY = 0;
    private int playerSize = 18;
    private int hp;

    // Câmera
    private double camX = 0;
    private double camY = 0;

    // Sprite e direção
    private CharacterSprite sprite;
    private Direction facing = Direction.DOWN;
    private double animTime = 0.0;

    private final Color bgColor = new Color(12, 12, 16);
    private final Color playerColor = new Color(80, 200, 160);

    // Render buffers
    private BufferedImage mapImage;        // pré-render do mapa (mundo inteiro)
    private BufferedImage vignetteImage;   // vinheta do tamanho do painel

    // Mouse e ataque
    private int mouseX = 0;
    private int mouseY = 0;
    private long lastAttackNs = 0L;
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<SlashEffect> slashEffects = new ArrayList<>();

    public GamePanel(PlayerClass playerClass, Runnable onExitToMenu) {
        this.playerClass = playerClass;
        this.onExitToMenu = onExitToMenu;
        this.hp = playerClass.getBaseHp();

        setFocusable(true);
        setBackground(bgColor);
        setDoubleBuffered(true);

        sprite = Sprites.forClass(playerClass);
        generateMap();
        buildMapImage();
        spawnPlayer();
        setupKeyBindings();
        setupMouseInput();

        // Recria vinheta ao redimensionar (para evitar custo por frame)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                rebuildVignette();
            }
        });
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Movimento contínuo com key pressed/released
        bindKey(im, am, KeyEvent.VK_W, "up-pressed", () -> velY = -playerClass.getMoveSpeed());
        bindKey(im, am, KeyEvent.VK_S, "down-pressed", () -> velY = playerClass.getMoveSpeed());
        bindKey(im, am, KeyEvent.VK_A, "left-pressed", () -> velX = -playerClass.getMoveSpeed());
        bindKey(im, am, KeyEvent.VK_D, "right-pressed", () -> velX = playerClass.getMoveSpeed());

        bindKey(im, am, KeyEvent.VK_W, "up-released", () -> { if (velY < 0) velY = 0; }, true);
        bindKey(im, am, KeyEvent.VK_S, "down-released", () -> { if (velY > 0) velY = 0; }, true);
        bindKey(im, am, KeyEvent.VK_A, "left-released", () -> { if (velX < 0) velX = 0; }, true);
        bindKey(im, am, KeyEvent.VK_D, "right-released", () -> { if (velX > 0) velX = 0; }, true);

        // ESC para voltar ao menu
        bindKey(im, am, KeyEvent.VK_ESCAPE, "escape", () -> {
            stopLoop();
            if (onExitToMenu != null) onExitToMenu.run();
        });
    }

    private void bindKey(InputMap im, ActionMap am, int keyCode, String name, Runnable action) {
        bindKey(im, am, keyCode, name, action, false);
    }

    private void bindKey(InputMap im, ActionMap am, int keyCode, String name, Runnable action, boolean released) {
        im.put(KeyStroke.getKeyStroke(keyCode, 0, released), name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        startLoop();
        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        stopLoop();
        super.removeNotify();
    }

    public void startLoop() {
        if (running) return;
        running = true;
        loopThread = new Thread(this, "GameLoop");
        loopThread.start();
    }

    public void stopLoop() {
        running = false;
        if (loopThread != null) {
            try { loopThread.join(200); } catch (InterruptedException ignored) {}
            loopThread = null;
        }
    }

    @Override
    public void run() {
        final int fps = 60;
        final long frameTimeMs = 1000L / fps;
        while (running) {
            long start = System.nanoTime();

            updateGame();
            repaint();

            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            long sleep = frameTimeMs - elapsedMs;
            if (sleep > 1) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            } else {
                Thread.yield();
            }
        }
    }

    private void updateGame() {
        // Movimento com colisão por tiles (separado por eixo)
        // Atualiza direção e animação com base no movimento (padrão)
        if (velX > 0) facing = Direction.RIGHT;
        else if (velX < 0) facing = Direction.LEFT;
        else if (velY > 0) facing = Direction.DOWN;
        else if (velY < 0) facing = Direction.UP;
        animTime += 1.0/60.0;

        double nextX = playerX + velX;
        if (!collides(nextX, playerY)) playerX = nextX;

        double nextY = playerY + velY;
        if (!collides(playerX, nextY)) playerY = nextY;

        // Atualiza câmera para centralizar no jogador
        camX = playerX - getWidth() / 2.0 + playerSize / 2.0;
        camY = playerY - getHeight() / 2.0 + playerSize / 2.0;

        // Limitar câmera aos limites do mapa
        int worldW = map.getWidth() * tileSize;
        int worldH = map.getHeight() * tileSize;
        camX = Math.max(0, Math.min(worldW - getWidth(), camX));
        camY = Math.max(0, Math.min(worldH - getHeight(), camY));

        // Atualiza projéteis
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update();
            if (p.dead || hitsWall(p.x, p.y)) {
                it.remove();
            }
        }

        // Atualiza efeitos de corte
        Iterator<SlashEffect> it2 = slashEffects.iterator();
        while (it2.hasNext()) {
            SlashEffect s = it2.next();
            if (!s.update()) it2.remove();
        }
    }

    private boolean collides(double x, double y) {
        int left = (int)Math.floor(x / tileSize);
        int right = (int)Math.floor((x + playerSize - 1) / tileSize);
        int top = (int)Math.floor(y / tileSize);
        int bottom = (int)Math.floor((y + playerSize - 1) / tileSize);

        for (int ty = top; ty <= bottom; ty++) {
            for (int tx = left; tx <= right; tx++) {
                if (tx < 0 || ty < 0 || tx >= map.getWidth() || ty >= map.getHeight()) return true;
                Tile t = map.get(tx, ty);
                if (t == Tile.WALL) return true;
            }
        }
        return false;
    }

    private void generateMap() {
        int tilesWide = 100;
        int tilesHigh = 70;
        long seed = System.currentTimeMillis();
        DungeonGenerator gen = new DungeonGenerator(tilesWide, tilesHigh, tileSize, seed);
        map = gen.generate();
    }

    private void spawnPlayer() {
        // Busca uma célula piso aproximada do centro
        int cx = map.getWidth() / 2;
        int cy = map.getHeight() / 2;
        int radius = Math.max(map.getWidth(), map.getHeight());
        boolean found = false;
        outer: for (int r = 0; r < radius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int tx = cx + dx; int ty = cy + dy;
                    if (tx < 0 || ty < 0 || tx >= map.getWidth() || ty >= map.getHeight()) continue;
                    if (map.get(tx, ty) == Tile.FLOOR) {
                        playerX = tx * tileSize + (tileSize - playerSize) / 2.0;
                        playerY = ty * tileSize + (tileSize - playerSize) / 2.0;
                        found = true;
                        break outer;
                    }
                }
            }
        }
        if (!found) {
            playerX = 2 * tileSize;
            playerY = 2 * tileSize;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fundo
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Desenha pré-render do mapa
        if (mapImage != null) {
            g2.drawImage(mapImage, (int) -camX, (int) -camY, null);
        }

        // Efeitos de corte (no espaço do mundo)
        for (SlashEffect s : slashEffects) {
            s.paint(g2, (float)(playerX - camX + playerSize/2.0), (float)(playerY - camY + playerSize/2.0));
        }

        // Projéteis
        for (Projectile p : projectiles) {
            p.paint(g2, (int)(p.x - camX), (int)(p.y - camY));
        }

        // Jogador com sprite
        if (sprite != null) {
            sprite.paint(g2, (int)(playerX - camX), (int)(playerY - camY), playerSize, facing, animTime);
        }

        // HUD
        g2.setColor(new Color(230, 230, 235));
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        String hud = String.format("Classe: %s | HP: %d | Vel: %.1f | ATK: %d | ESC: Menu",
                playerClass.getDisplayName(), hp, playerClass.getMoveSpeed(), playerClass.getAttackDamage());
        g2.drawString(hud, 12, 20);

        // Vinheta sutil (pré-gerada)
        drawVignette(g2);

        g2.dispose();
    }

    private void buildMapImage() {
        int worldW = map.getWidth() * tileSize;
        int worldH = map.getHeight() * tileSize;
        mapImage = new BufferedImage(worldW, worldH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = mapImage.createGraphics();
        // Anti-alias não é necessário para tiles retangulares; prioriza performance
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int w = map.getWidth();
        int h = map.getHeight();
        Color wall = new Color(45, 49, 66);
        Color wallShadow = new Color(25, 28, 38);
        Color floorBase = new Color(62, 68, 89);
        Color floorAlt = new Color(66, 72, 95);
        Color doorColor = new Color(120, 98, 70);
        Color pebble = new Color(50, 55, 72);

        for (int ty = 0; ty < h; ty++) {
            for (int tx = 0; tx < w; tx++) {
                Tile t = map.get(tx, ty);
                int px = tx * tileSize;
                int py = ty * tileSize;

                switch (t) {
                    case FLOOR: {
                        float r = map.random01(tx, ty);
                        Color c = mix(floorBase, floorAlt, r * 0.5f);
                        g2.setColor(c);
                        g2.fillRect(px, py, tileSize, tileSize);

                        // detalhes de pedras
                        g2.setColor(pebble);
                        if (r > 0.85f) g2.fillRect(px + 4, py + 6, 2, 2);
                        if (r < 0.15f) g2.fillRect(px + 12, py + 12, 2, 2);
                        break;
                    }
                    case WALL: {
                        g2.setColor(wall);
                        g2.fillRect(px, py, tileSize, tileSize);
                        // Top highlight se acima for piso
                        if (ty + 1 < h && map.get(tx, ty + 1) == Tile.FLOOR) {
                            g2.setColor(wallShadow);
                            g2.fillRect(px, py + tileSize - 5, tileSize, 5);
                        }
                        break;
                    }
                    case DOOR: {
                        g2.setColor(doorColor);
                        g2.fillRect(px, py, tileSize, tileSize);
                        g2.setColor(doorColor.darker());
                        g2.fillRect(px + 2, py + 2, tileSize - 4, tileSize - 4);
                        break;
                    }
                }
            }
        }

        g2.dispose();
    }

    private Color mix(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int)(a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int)(a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int)(a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, g, bl);
    }

    private void drawVignette(Graphics2D g2) {
        if (vignetteImage == null || vignetteImage.getWidth() != getWidth() || vignetteImage.getHeight() != getHeight()) {
            rebuildVignette();
        }
        if (vignetteImage != null) {
            g2.drawImage(vignetteImage, 0, 0, null);
        }
    }

    private void rebuildVignette() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Paint old = g.getPaint();
        RadialGradientPaint rg = new RadialGradientPaint(
                new Point(w/2, h/2), Math.max(w, h) * 0.7f,
                new float[]{0f, 1f},
                new Color[]{new Color(0,0,0,0), new Color(0, 0, 0, 110)}
        );
        g.setPaint(rg);
        g.fillRect(0, 0, w, h);
        g.setPaint(old);
        g.dispose();
        vignetteImage = img;
    }

    // ========= Mouse e ataque =========
    private void setupMouseInput() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseX = e.getX();
                    mouseY = e.getY();
                    tryAttack();
                }
            }
        });
    }

    private void tryAttack() {
        long now = System.nanoTime();
        long cd = attackCooldownNanos();
        if (now - lastAttackNs < cd) return;
        lastAttackNs = now;

        double px = playerX + playerSize/2.0;
        double py = playerY + playerSize/2.0;
        double mx = camX + mouseX;
        double my = camY + mouseY;
        double ang = Math.atan2(my - py, mx - px);

        // Ajusta facing ao quadrante do mouse
        double a = Math.toDegrees(ang);
        if (a >= -45 && a < 45) facing = Direction.RIGHT;
        else if (a >= 45 && a < 135) facing = Direction.DOWN;
        else if (a >= -135 && a < -45) facing = Direction.UP;
        else facing = Direction.LEFT;

        switch (playerClass) {
            case WARRIOR:
                spawnSlash(ang);
                break;
            case MAGE:
                spawnProjectile(ang, 6.0, 180, new Color(140, 200, 255), 5);
                break;
            case ROGUE:
                spawnProjectile(ang, 7.5, 160, new Color(210, 220, 220), 3);
                break;
            case RANGER:
                spawnProjectile(ang, 7.0, 220, new Color(240, 210, 160), 4);
                break;
            case CLERIC:
                spawnProjectile(ang, 5.5, 200, new Color(255, 235, 150), 6);
                break;
        }
    }

    private long attackCooldownNanos() {
        long ms;
        switch (playerClass) {
            case WARRIOR: ms = 400; break;
            case MAGE: ms = 600; break;
            case ROGUE: ms = 250; break;
            case RANGER: ms = 500; break;
            case CLERIC: ms = 700; break;
            default: ms = 500; break;
        }
        return ms * 1_000_000L;
    }

    private boolean hitsWall(double x, double y) {
        int tx = (int)Math.floor(x / tileSize);
        int ty = (int)Math.floor(y / tileSize);
        if (tx < 0 || ty < 0 || tx >= map.getWidth() || ty >= map.getHeight()) return true;
        return map.get(tx, ty) == Tile.WALL;
    }

    private void spawnProjectile(double ang, double speed, int lifeFrames, Color color, int radius) {
        double px = playerX + playerSize/2.0;
        double py = playerY + playerSize/2.0;
        double vx = Math.cos(ang) * speed;
        double vy = Math.sin(ang) * speed;
        projectiles.add(new Projectile(px, py, vx, vy, lifeFrames, color, radius));
    }

    private void spawnSlash(double ang) {
        SlashEffect s = new SlashEffect(ang, 14, tileSize * 1.2f);
        slashEffects.add(s);
    }

    // ========= Tipos auxiliares =========
    private static class Projectile {
        double x, y, vx, vy;
        int life;
        boolean dead = false;
        final Color color;
        final int radius;

        Projectile(double x, double y, double vx, double vy, int lifeFrames, Color color, int radius) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = lifeFrames; this.color = color; this.radius = radius;
        }

        void update() {
            x += vx; y += vy;
            if (--life <= 0) dead = true;
        }

        void paint(Graphics2D g2, int sx, int sy) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 220));
            g2.fillOval(sx - radius, sy - radius, radius*2, radius*2);
        }
    }

    private static class SlashEffect {
        final double ang;
        int life;
        final float radius;

        SlashEffect(double ang, int lifeFrames, float radius) {
            this.ang = ang; this.life = lifeFrames; this.radius = radius;
        }

        boolean update() { return --life > 0; }

        void paint(Graphics2D g2, float cx, float cy) {
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 120));
            float a0 = (float)Math.toDegrees(ang) - 30f;
            g2.drawArc((int)(cx - radius), (int)(cy - radius), (int)(radius*2), (int)(radius*2), (int)a0, 60);
            g2.setStroke(old);
        }
    }
}
