package com.mycompany.backgammon.client;

import javax.swing.*;
import java.awt.*;

public class DicePanel extends JPanel {

    private int die1 = 0;
    private int die2 = 0;

    private static final int DIE_SIZE = 60;
    private static final int GAP = 16;
    private static final int DOT_SIZE = 10;
    private static final Color DIE_BG = Color.WHITE;
    private static final Color DIE_BORDER = new Color(0x3D2313);
    private static final Color DOT_COLOR = Color.BLACK;

    public DicePanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(DIE_SIZE * 2 + GAP + 20, DIE_SIZE + 20));
    }

    public void setDice(int d1, int d2) {
        this.die1 = d1;
        this.die2 = d2;
        repaint();
    }

    public void clearDice() {
        this.die1 = 0;
        this.die2 = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (die1 == 0 && die2 == 0) return;

        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int totalW = DIE_SIZE * 2 + GAP;
        int startX = (getWidth() - totalW) / 2;
        int startY = (getHeight() - DIE_SIZE) / 2;

        drawDie(g, startX, startY, die1);
        drawDie(g, startX + DIE_SIZE + GAP, startY, die2);

        g.dispose();
    }

    private void drawDie(Graphics2D g, int x, int y, int value) {
        g.setColor(DIE_BG);
        g.fillRoundRect(x, y, DIE_SIZE, DIE_SIZE, 12, 12);
        g.setColor(DIE_BORDER);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(x, y, DIE_SIZE, DIE_SIZE, 12, 12);

        g.setColor(DOT_COLOR);
        int cx = x + DIE_SIZE / 2;
        int cy = y + DIE_SIZE / 2;
        int off = DIE_SIZE / 4;

        switch (value) {
            case 1 -> dot(g, cx, cy);
            case 2 -> {
                dot(g, cx - off, cy - off);
                dot(g, cx + off, cy + off);
            }
            case 3 -> {
                dot(g, cx - off, cy - off);
                dot(g, cx, cy);
                dot(g, cx + off, cy + off);
            }
            case 4 -> {
                dot(g, cx - off, cy - off);
                dot(g, cx + off, cy - off);
                dot(g, cx - off, cy + off);
                dot(g, cx + off, cy + off);
            }
            case 5 -> {
                dot(g, cx - off, cy - off);
                dot(g, cx + off, cy - off);
                dot(g, cx, cy);
                dot(g, cx - off, cy + off);
                dot(g, cx + off, cy + off);
            }
            case 6 -> {
                dot(g, cx - off, cy - off);
                dot(g, cx + off, cy - off);
                dot(g, cx - off, cy);
                dot(g, cx + off, cy);
                dot(g, cx - off, cy + off);
                dot(g, cx + off, cy + off);
            }
        }
    }

    private void dot(Graphics2D g, int cx, int cy) {
        g.fillOval(cx - DOT_SIZE / 2, cy - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE);
    }
}
