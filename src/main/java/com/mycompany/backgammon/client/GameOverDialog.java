package com.mycompany.backgammon.client;

import com.mycompany.backgammon.game.GameState;
import com.mycompany.backgammon.game.Player;

import javax.swing.*;
import java.awt.*;

/**
 * Custom game-over dialog: shows winner, score summary, and replay/quit choice.
 */
public class GameOverDialog extends JDialog {

    private boolean wantsReplay = false;

    public GameOverDialog(JFrame parent, Player winner, Player myColor, GameState state) {
        super(parent, true); // modal
        setUndecorated(false);
        setResizable(false);

        String winnerName = (winner == Player.WHITE)
                ? (state != null ? state.whiteName : "White") + " (Beyaz)"
                : (state != null ? state.blackName : "Black") + " (Siyah)";
        boolean iWon = (winner == myColor);
        String headline = iWon ? "Tebrikler, Kazandınız! 🏆" : "Oyun Bitti";

        setTitle(iWon ? "Kazandınız!" : "Kaybettiniz");

        JPanel root = new JPanel(new BorderLayout(12, 12)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0xFFF8EE),
                        0, getHeight(), new Color(0xE8C88A)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Headline
        JLabel headlineLabel = new JLabel(headline, SwingConstants.CENTER);
        headlineLabel.setFont(new Font("Serif", Font.BOLD, 26));
        headlineLabel.setForeground(iWon ? new Color(0x4A7C0F) : new Color(0x8B1A1A));
        root.add(headlineLabel, BorderLayout.NORTH);

        // Center: winner + score
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel winnerLabel = new JLabel("Kazanan: " + winnerName, SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Serif", Font.BOLD, 20));
        winnerLabel.setForeground(new Color(0x5A3200));
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(winnerLabel);
        center.add(Box.createVerticalStrut(12));

        if (state != null) {
            JLabel scoreLabel = new JLabel(
                    "Toplanan taş — Beyaz: " + state.off[0] + "  Siyah: " + state.off[1],
                    SwingConstants.CENTER);
            scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            scoreLabel.setForeground(new Color(0x3D2313));
            scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(scoreLabel);

            if (state.bar[0] > 0 || state.bar[1] > 0) {
                JLabel barLabel = new JLabel(
                        "Bar'da kalan — Beyaz: " + state.bar[0] + "  Siyah: " + state.bar[1],
                        SwingConstants.CENTER);
                barLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
                barLabel.setForeground(new Color(0x666666));
                barLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                center.add(Box.createVerticalStrut(4));
                center.add(barLabel);
            }
        }
        root.add(center, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setOpaque(false);

        JButton replayBtn = new JButton("Tekrar Oyna");
        replayBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        replayBtn.setBackground(new Color(0x4A7C0F));
        replayBtn.setForeground(Color.WHITE);
        replayBtn.setFocusPainted(false);
        replayBtn.addActionListener(e -> { wantsReplay = true; dispose(); });

        JButton quitBtn = new JButton("Cikis");
        quitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        quitBtn.setBackground(new Color(0x8B1A1A));
        quitBtn.setForeground(Color.WHITE);
        quitBtn.setFocusPainted(false);
        quitBtn.addActionListener(e -> { wantsReplay = false; dispose(); });

        btnPanel.add(replayBtn);
        btnPanel.add(quitBtn);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(380, 220));
        setLocationRelativeTo(parent);
    }

    public boolean wantsReplay() { return wantsReplay; }
}
