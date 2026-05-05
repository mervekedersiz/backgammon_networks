package com.mycompany.backgammon.client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws remaining dice for the current turn.
 * For normal rolls: shows 1 or 2 dice.
 * For doubles: shows up to 4 dice (one per remaining move).
 * Used dice disappear from the display automatically.
 */
public class DicePanel extends JPanel {

    private static final int DIE_SIZE = 48;
    private static final int GAP     = 8;
    private static final int DOT     = 8;
    private static final Color DIE_BG     = Color.WHITE;
    private static final Color DIE_BORDER = new Color(0x3D2313);
    private static final Color DOT_COLOR  = new Color(0x1A1A1A);

    private List<Integer> dicesToShow = new ArrayList<>();

    public DicePanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(DIE_SIZE * 4 + GAP * 3 + 20, DIE_SIZE + 20));
    }

    /** Called on each state update: show only the remaining (unused) dice. */
    public void setRemainingDice(List<Integer> remaining) {
        this.dicesToShow = new ArrayList<>(remaining);
        repaint();
    }

    /** Kept for compatibility (shows original two dice). */
    public void setDice(int d1, int d2) {
        dicesToShow = new ArrayList<>();
        if (d1 > 0) dicesToShow.add(d1);
        if (d2 > 0) dicesToShow.add(d2);
        repaint();
    }

    public void clearDice() {
        dicesToShow.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (dicesToShow.isEmpty()) return;

        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int n = dicesToShow.size();
        int totalW = n * DIE_SIZE + (n - 1) * GAP;
        int startX = (getWidth() - totalW) / 2;
        int startY = (getHeight() - DIE_SIZE) / 2;

        for (int i = 0; i < n; i++) {
            drawDie(g, startX + i * (DIE_SIZE + GAP), startY, dicesToShow.get(i));
        }
        g.dispose();
    }

    private void drawDie(Graphics2D g, int x, int y, int value) {
        g.setColor(DIE_BG);
        g.fillRoundRect(x, y, DIE_SIZE, DIE_SIZE, 10, 10);
        g.setColor(DIE_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, DIE_SIZE, DIE_SIZE, 10, 10);

        g.setColor(DOT_COLOR);
        int cx = x + DIE_SIZE / 2;
        int cy = y + DIE_SIZE / 2;
        int off = DIE_SIZE / 4;

        switch (value) {
            case 1 -> dot(g, cx, cy);
            case 2 -> { dot(g, cx - off, cy - off); dot(g, cx + off, cy + off); }
            case 3 -> { dot(g, cx - off, cy - off); dot(g, cx, cy); dot(g, cx + off, cy + off); }
            case 4 -> { dot(g, cx - off, cy - off); dot(g, cx + off, cy - off);
                        dot(g, cx - off, cy + off); dot(g, cx + off, cy + off); }
            case 5 -> { dot(g, cx - off, cy - off); dot(g, cx + off, cy - off); dot(g, cx, cy);
                        dot(g, cx - off, cy + off); dot(g, cx + off, cy + off); }
            case 6 -> { dot(g, cx - off, cy - off); dot(g, cx + off, cy - off);
                        dot(g, cx - off, cy);       dot(g, cx + off, cy);
                        dot(g, cx - off, cy + off); dot(g, cx + off, cy + off); }
        }
    }

    private void dot(Graphics2D g, int cx, int cy) {
        g.fillOval(cx - DOT / 2, cy - DOT / 2, DOT, DOT);
    }
}
