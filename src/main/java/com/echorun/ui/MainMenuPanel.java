package com.echorun.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class MainMenuPanel extends JPanel {
    public interface Action {
        void run();
    }

    private final Action onPlay;
    private final Action onSettings;

    public MainMenuPanel(Action onPlay, Action onSettings) {
        this.onPlay = Objects.requireNonNull(onPlay);
        this.onSettings = Objects.requireNonNull(onSettings);
        buildUi();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        setBackground(new Color(18, 18, 22));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel title = new JLabel("ECHO_RUN");
        title.setForeground(new Color(240, 240, 240));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 48f));

        JButton play = new JButton("Jogar");
        play.setPreferredSize(new Dimension(220, 48));
        play.setFont(play.getFont().deriveFont(Font.BOLD, 18f));
        play.addActionListener(e -> onPlay.run());

        JButton settings = new JButton("Configurações");
        settings.setPreferredSize(new Dimension(220, 48));
        settings.setFont(settings.getFont().deriveFont(Font.PLAIN, 18f));
        settings.addActionListener(e -> onSettings.run());

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new GridLayout(0, 1, 0, 12));
        buttons.add(play);
        buttons.add(settings);

        // Add components stacked vertically
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);

        stack.add(title);
        stack.add(Box.createVerticalStrut(24));
        stack.add(buttons);

        add(stack, gbc);
    }
}
