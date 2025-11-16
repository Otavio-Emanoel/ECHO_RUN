package com.echorun.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel implements Runnable {
    private final PlayerClass playerClass;
    private final Runnable onExitToMenu;

    private Thread loopThread;
    private volatile boolean running = false;

    // Mundo simples
    private double playerX = 100;
    private double playerY = 100;
    private double velX = 0;
    private double velY = 0;
    private int playerSize = 32;
    private int hp;

    private final Color bgColor = new Color(18, 18, 22);
    private final Color playerColor = new Color(80, 200, 120);

    public GamePanel(PlayerClass playerClass, Runnable onExitToMenu) {
        this.playerClass = playerClass;
        this.onExitToMenu = onExitToMenu;
        this.hp = playerClass.getBaseHp();

        setFocusable(true);
        setBackground(bgColor);
        setDoubleBuffered(true);

        setupKeyBindings();
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
        final long frameTime = 1000L / fps; // ms
        while (running) {
            long start = System.currentTimeMillis();
            updateGame();
            repaint();
            long elapsed = System.currentTimeMillis() - start;
            long sleep = frameTime - elapsed;
            if (sleep < 2) sleep = 2;
            try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
        }
    }

    private void updateGame() {
        playerX += velX;
        playerY += velY;

        // Limites da tela
        int w = getWidth();
        int h = getHeight();
        if (playerX < 0) playerX = 0;
        if (playerY < 0) playerY = 0;
        if (playerX > w - playerSize) playerX = w - playerSize;
        if (playerY > h - playerSize) playerY = h - playerSize;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fundo
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Chão simples
        g2.setColor(new Color(40, 40, 48));
        g2.fillRect(0, getHeight() - 80, getWidth(), 80);

        // Jogador
        g2.setColor(playerColor);
        g2.fillRoundRect((int) playerX, (int) playerY, playerSize, playerSize, 10, 10);

        // HUD
        g2.setColor(new Color(230, 230, 235));
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        String hud = String.format("Classe: %s | HP: %d | Vel: %.1f | ATK: %d | ESC: Menu",
                playerClass.getDisplayName(), hp, playerClass.getMoveSpeed(), playerClass.getAttackDamage());
        g2.drawString(hud, 12, 20);

        g2.dispose();
    }
}
