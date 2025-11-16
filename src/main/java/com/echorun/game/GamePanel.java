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

    private final Color bgColor = new Color(12, 12, 16);
    private final Color playerColor = new Color(80, 200, 160);

    // Render buffers
    private BufferedImage mapImage;        // pré-render do mapa (mundo inteiro)
    private BufferedImage vignetteImage;   // vinheta do tamanho do painel

    public GamePanel(PlayerClass playerClass, Runnable onExitToMenu) {
        this.playerClass = playerClass;
        this.onExitToMenu = onExitToMenu;
        this.hp = playerClass.getBaseHp();

        setFocusable(true);
        setBackground(bgColor);
        setDoubleBuffered(true);

        generateMap();
        buildMapImage();
        spawnPlayer();
        setupKeyBindings();

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

        // Jogador (aplicando offset da câmera para mantê-lo visível/centralizado)
        g2.setColor(playerColor);
        g2.fillRoundRect((int) (playerX - camX), (int) (playerY - camY), playerSize, playerSize, 8, 8);

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
}
