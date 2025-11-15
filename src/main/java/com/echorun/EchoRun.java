package com.echorun;

import com.echorun.ui.MainMenuPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EchoRun {
    private JFrame frame;
    private GraphicsDevice graphicsDevice;

    private boolean isFullscreen = false;
    private Dimension windowedSize;
    private Point windowedLocation;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EchoRun::new);
    }

    public EchoRun() {
        setSystemLookAndFeel();
        initWindow();
    }

    private void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void initWindow() {
        frame = new JFrame("ECHO_RUN");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(960, 540));
        frame.setSize(new Dimension(1280, 720));
        frame.setLocationRelativeTo(null);

        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        // Top bar with fullscreen toggle (right-aligned)
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JToggleButton fullscreenToggle = new JToggleButton("Tela cheia");
        fullscreenToggle.setFocusable(false);
        fullscreenToggle.addActionListener(e -> toggleFullscreen(fullscreenToggle.isSelected()));
        topBar.add(fullscreenToggle);

        // Main content
        MainMenuPanel menu = new MainMenuPanel(
                () -> onPlay(),
                () -> onOpenSettings()
        );

        frame.add(topBar, BorderLayout.NORTH);
        frame.add(menu, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                frame.requestFocusInWindow();
            }
        });

        frame.setVisible(true);
    }

    private void onPlay() {
        JOptionPane.showMessageDialog(frame, "Iniciar jogo em breve!", "Jogar", JOptionPane.INFORMATION_MESSAGE);
        // TODO: trocar para a cena do jogo quando implementado.
    }

    private void onOpenSettings() {
        JOptionPane.showMessageDialog(frame, "Tela de Configurações em breve!", "Configurações", JOptionPane.INFORMATION_MESSAGE);
        // TODO: abrir painel de configurações real quando implementado.
    }

    private void toggleFullscreen(boolean enable) {
        if (enable == isFullscreen) return;
        isFullscreen = enable;

        if (enable) {
            // Save current window bounds
            windowedSize = frame.getSize();
            windowedLocation = frame.getLocation();

            try {
                frame.dispose();
                frame.setUndecorated(true);
                frame.setResizable(false);

                if (graphicsDevice.isFullScreenSupported()) {
                    graphicsDevice.setFullScreenWindow(frame);
                } else {
                    // Fallback: maximize
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            } finally {
                frame.setVisible(true);
            }
        } else {
            try {
                if (graphicsDevice.getFullScreenWindow() == frame) {
                    graphicsDevice.setFullScreenWindow(null);
                }
                frame.dispose();
                frame.setUndecorated(false);
                frame.setResizable(true);
                if (windowedSize != null) frame.setSize(windowedSize);
                if (windowedLocation != null) frame.setLocation(windowedLocation);
                frame.setExtendedState(JFrame.NORMAL);
            } finally {
                frame.setVisible(true);
            }
        }
    }
}
