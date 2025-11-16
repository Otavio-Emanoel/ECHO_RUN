package com.echorun.ui;

import com.echorun.game.PlayerClass;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class CharacterSelectPanel extends JPanel {

    public CharacterSelectPanel(Consumer<PlayerClass> onSelect, Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 24));

        JLabel title = new JLabel("Escolha sua Classe", SwingConstants.CENTER);
        title.setForeground(new Color(240, 240, 240));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 36f));
        title.setBorder(BorderFactory.createEmptyBorder(24, 12, 12, 12));

        JPanel grid = new JPanel(new GridLayout(0, 3, 16, 16));
        grid.setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));
        grid.setOpaque(false);

        for (PlayerClass pc : PlayerClass.values()) {
            grid.add(createClassCard(pc, onSelect));
        }

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.CENTER;
        centerWrap.add(grid, gbc);

        JButton back = new JButton("Voltar");
        back.addActionListener(e -> { if (onBack != null) onBack.run(); });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(back);

        add(title, BorderLayout.NORTH);
        add(centerWrap, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private JComponent createClassCard(PlayerClass pc, java.util.function.Consumer<PlayerClass> onSelect) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 90)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setBackground(new Color(30, 30, 36));

        JLabel name = new JLabel(pc.getDisplayName(), SwingConstants.CENTER);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setForeground(new Color(230, 230, 235));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 18f));

        JLabel stats = new JLabel(
                String.format("HP: %d  |  Vel: %.1f  |  ATK: %d",
                        pc.getBaseHp(), pc.getMoveSpeed(), pc.getAttackDamage()),
                SwingConstants.CENTER);
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        stats.setForeground(new Color(200, 200, 205));

        JButton choose = new JButton("Escolher");
        choose.setAlignmentX(Component.CENTER_ALIGNMENT);
        choose.addActionListener(e -> onSelect.accept(pc));

        card.add(name);
        card.add(Box.createVerticalStrut(8));
        card.add(stats);
        card.add(Box.createVerticalStrut(12));
        card.add(choose);

        return card;
    }
}
