package com.mycompany.backgammon.client;

import com.mycompany.backgammon.game.GameState;
import com.mycompany.backgammon.game.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Renders a backgammon board and reports click targets as point indices.
 *
 * Layout (from white's perspective — bottom half is white's home):
 *   Top half indices (L->R):    12 13 14 15 16 17 | BAR | 18 19 20 21 22 23
 *   Bottom half indices (L->R): 11 10  9  8  7  6 | BAR |  5  4  3  2  1  0
 * Off-tray drawn on the far right edge; bar drawn in the middle.
 *
 * Click targets:
 *   0..23 : normal points
 *   -1    : "from bar" when clicking the bar; or "bear off" when clicking tray
 *   Differentiated via callback: onPointClicked for points, onBarClicked,
 *   onOffClicked.
 */
public class BoardPanel extends JPanel {

    private GameState state;
    private int selectedFrom = Integer.MIN_VALUE; // -2 sentinel = none; -1 = bar selected
    private boolean showDebugLabels = false;
    private final Set<Integer> legalTargets = new LinkedHashSet<>();

    private IntConsumer onPointClicked = p -> {};
    private Runnable onBarClicked = () -> {};
    private Runnable onOffClicked = () -> {};

    private static final Color WOOD        = new Color(0xC8A063);
    private static final Color WOOD_DARK   = new Color(0x8B5A2B);
    private static final Color TRI_LIGHT   = new Color(0xF1D9A8);
    private static final Color TRI_DARK    = new Color(0x704214);
    private static final Color FRAME       = new Color(0x3D2313);
    private static final Color WHITE_C     = new Color(0xF5F5F5);
    private static final Color BLACK_C     = new Color(0x1A1A1A);
    private static final Color TARGET_C    = new Color(0x37C871);

    public BoardPanel() {
        setBackground(FRAME);
        setPreferredSize(new Dimension(900, 560));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
    }

    public void setState(GameState s)          { this.state = s; repaint(); }
    public void setSelected(int idx)           { this.selectedFrom = idx; repaint(); }
    public void setLegalTargets(Set<Integer> targets) {
        legalTargets.clear();
        if (targets != null) legalTargets.addAll(targets);
        repaint();
    }
    public void clearSelection() {
        this.selectedFrom = Integer.MIN_VALUE;
        legalTargets.clear();
        repaint();
    }

    public void setOnPointClicked(IntConsumer c) { this.onPointClicked = c; }
    public void setOnBarClicked(Runnable r)      { this.onBarClicked = r; }
    public void setOnOffClicked(Runnable r)      { this.onOffClicked = r; }

    /* --------------- geometry --------------- */

    private Rectangle boardRect() {
        int pad = 20;
        return new Rectangle(pad, pad, getWidth() - 2 * pad, getHeight() - 2 * pad);
    }

    private int barWidth(Rectangle b)  { return Math.max(30, b.width / 26); }
    private int trayWidth(Rectangle b) { return Math.max(40, b.width / 18); }

    private int pointWidth(Rectangle b) {
        return (b.width - barWidth(b) - trayWidth(b)) / 12;
    }

    /** Column 0..11 left-to-right on the top, and 0..11 on the bottom. */
    private Rectangle pointRect(int col, boolean top) {
        Rectangle b = boardRect();
        int pw = pointWidth(b);
        int h = b.height / 2;
        int x = b.x + col * pw;
        if (col >= 6) x += barWidth(b);
        int y = top ? b.y : b.y + h;
        return new Rectangle(x, y, pw, h);
    }

    private Rectangle barRect() {
        Rectangle b = boardRect();
        int pw = pointWidth(b);
        int x = b.x + 6 * pw;
        return new Rectangle(x, b.y, barWidth(b), b.height);
    }

    private Rectangle trayRect() {
        Rectangle b = boardRect();
        int pw = pointWidth(b);
        int x = b.x + 12 * pw + barWidth(b);
        return new Rectangle(x, b.y, trayWidth(b), b.height);
    }

    /**
     * Maps board column (0..11) and half (top/bottom) to an absolute point index.
     *
     * WHITE perspective (default):
     *   top    L→R: 12,13,14,15,16,17 | BAR | 18,19,20,21,22,23
     *   bottom L→R: 11,10, 9, 8, 7, 6 | BAR |  5, 4, 3, 2, 1, 0
     *
     * BLACK perspective (rotated so black's home 18–23 is at bottom-right):
     *   top    L→R:  6, 7, 8, 9,10,11 | BAR |  5, 4, 3, 2, 1, 0
     *   bottom L→R: 17,16,15,14,13,12 | BAR | 18,19,20,21,22,23
     */
    private int colToIndex(int col, boolean top) {
        // Both players see the same board layout (White's perspective):
        //   top    L→R: 12,13,14,15,16,17 | BAR | 18,19,20,21,22,23
        //   bottom L→R: 11,10, 9, 8, 7, 6 | BAR |  5, 4, 3, 2, 1, 0
        return top ? (12 + col) : (11 - col);
    }

    /* --------------- click handling --------------- */

    private void handleClick(int x, int y) {
        if (state == null) return;
        Rectangle bar = barRect();
        if (bar.contains(x, y)) { onBarClicked.run(); return; }
        Rectangle tray = trayRect();
        if (tray.contains(x, y)) { onOffClicked.run(); return; }

        Rectangle b = boardRect();
        if (!b.contains(x, y)) return;
        boolean top = y < b.y + b.height / 2;
        int pw = pointWidth(b);
        int rx = x - b.x;
        if (rx >= 6 * pw && rx < 6 * pw + barWidth(b)) return; // clicked on bar strip
        if (rx >= 6 * pw + barWidth(b)) rx -= barWidth(b);
        int col = rx / pw;
        if (col < 0 || col > 11) return;
        onPointClicked.accept(colToIndex(col, top));
    }

    /* --------------- drawing --------------- */

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (state == null) return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle b = boardRect();
        g.setColor(WOOD);
        g.fillRect(b.x, b.y, b.width, b.height);
        g.setColor(FRAME);
        g.setStroke(new BasicStroke(6f));
        g.drawRect(b.x, b.y, b.width, b.height);
        g.setStroke(new BasicStroke(1f));

        // triangles
        for (int col = 0; col < 12; col++) {
            drawTriangle(g, col, true);
            drawTriangle(g, col, false);
        }

        // bar
        Rectangle bar = barRect();
        g.setColor(WOOD_DARK);
        g.fillRect(bar.x, bar.y, bar.width, bar.height);

        // tray
        Rectangle tray = trayRect();
        g.setColor(WOOD_DARK);
        g.fillRect(tray.x, tray.y, tray.width, tray.height);
        g.setColor(Color.BLACK);
        g.drawLine(tray.x, tray.y + tray.height / 2, tray.x + tray.width, tray.y + tray.height / 2);

        // checkers on points
        for (int col = 0; col < 12; col++) {
            drawCheckersAt(g, col, true);
            drawCheckersAt(g, col, false);
        }

        // bar checkers
        drawBarCheckers(g);
        // tray (borne off)
        drawTrayCheckers(g);

        // legal target and selected highlights
        drawLegalTargets(g);
        if (selectedFrom != Integer.MIN_VALUE) drawSelection(g);

        // status bar
        drawStatusBar(g);

        g.dispose();
    }

    private void drawTriangle(Graphics2D g, int col, boolean top) {
        Rectangle r = pointRect(col, top);
        int[] xs, ys;
        int tipY = top ? r.y + r.height - 4 : r.y + 4;
        if (top) {
            xs = new int[]{r.x + 2, r.x + r.width - 2, r.x + r.width / 2};
            ys = new int[]{r.y, r.y, tipY};
        } else {
            xs = new int[]{r.x + 2, r.x + r.width - 2, r.x + r.width / 2};
            ys = new int[]{r.y + r.height, r.y + r.height, tipY};
        }
        // alternate colors per column
        boolean light = ((col + (top ? 0 : 1)) % 2) == 0;
        g.setColor(light ? TRI_LIGHT : TRI_DARK);
        g.fillPolygon(xs, ys, 3);
        g.setColor(Color.BLACK);
        g.drawPolygon(xs, ys, 3);

        if (showDebugLabels) {
            int idx = colToIndex(col, top);
            g.setColor(new Color(0, 0, 0, 120));
            g.setFont(g.getFont().deriveFont(10f));
            String label = String.valueOf(idx);
            int lx = r.x + r.width / 2 - g.getFontMetrics().stringWidth(label) / 2;
            int ly = top ? r.y + 12 : r.y + r.height - 4;
            g.drawString(label, lx, ly);
        }
    }

    private void drawCheckersAt(Graphics2D g, int col, boolean top) {
        int idx = colToIndex(col, top);
        int count = Math.abs(state.points[idx]);
        if (count == 0) return;
        Color c = state.points[idx] > 0 ? WHITE_C : BLACK_C;
        Rectangle r = pointRect(col, top);
        int size = Math.min(r.width - 6, r.height / 6);
        int x = r.x + r.width / 2 - size / 2;
        int maxStack = 5;
        for (int i = 0; i < Math.min(count, maxStack); i++) {
            int y = top ? r.y + i * size : r.y + r.height - (i + 1) * size;
            fillChecker(g, x, y, size, c);
        }
        if (count > maxStack) {
            g.setColor(count > 0 && state.points[idx] > 0 ? Color.BLACK : Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
            String txt = String.valueOf(count);
            int ty = top ? r.y + (maxStack - 1) * size + size / 2 + 5
                         : r.y + r.height - (maxStack - 1) * size - size / 2 + 5;
            int tx = x + size / 2 - g.getFontMetrics().stringWidth(txt) / 2;
            g.drawString(txt, tx, ty);
        }
    }

    private void drawBarCheckers(Graphics2D g) {
        Rectangle bar = barRect();
        int size = Math.min(bar.width - 4, 30);
        int x = bar.x + bar.width / 2 - size / 2;
        int whiteCount = state.bar[0];
        int blackCount = state.bar[1];
        int midY = bar.y + bar.height / 2;
        // White bar checkers in upper half, Black in lower half (fixed for both viewers)
        for (int i = 0; i < whiteCount; i++) {
            int y = midY - (i + 1) * size - 4;
            fillChecker(g, x, y, size, WHITE_C);
        }
        for (int i = 0; i < blackCount; i++) {
            int y = midY + i * size + 4;
            fillChecker(g, x, y, size, BLACK_C);
        }
    }

    private void drawTrayCheckers(Graphics2D g) {
        Rectangle tray = trayRect();
        int size = Math.min(tray.width - 8, 18);
        int whiteOff = state.off[0];
        int blackOff = state.off[1];
        // White borne off in bottom half, Black borne off in top half (fixed for both viewers)
        for (int i = 0; i < whiteOff; i++) {
            int y = tray.y + tray.height - (i + 1) * (size + 1) - 4;
            fillChecker(g, tray.x + (tray.width - size) / 2, y, size, WHITE_C);
        }
        for (int i = 0; i < blackOff; i++) {
            int y = tray.y + i * (size + 1) + 4;
            fillChecker(g, tray.x + (tray.width - size) / 2, y, size, BLACK_C);
        }
    }

    private void fillChecker(Graphics2D g, int x, int y, int size, Color c) {
        g.setColor(c);
        g.fillOval(x, y, size, size);
        g.setColor(Color.DARK_GRAY);
        g.drawOval(x, y, size, size);
    }

    private void drawLegalTargets(Graphics2D g) {
        if (legalTargets.isEmpty()) return;
        Stroke oldStroke = g.getStroke();
        g.setColor(TARGET_C);
        g.setStroke(new BasicStroke(4f));
        for (int target : legalTargets) {
            if (target == -1) {
                Rectangle tray = trayRect();
                g.drawRect(tray.x + 4, tray.y + 4, tray.width - 8, tray.height - 8);
                continue;
            }
            for (int col = 0; col < 12; col++) {
                for (boolean top : new boolean[]{true, false}) {
                    if (colToIndex(col, top) == target) {
                        Rectangle r = pointRect(col, top);
                        g.drawRect(r.x + 5, r.y + 5, r.width - 10, r.height - 10);
                    }
                }
            }
        }
        g.setStroke(oldStroke);
    }

    private void drawSelection(Graphics2D g) {
        g.setColor(new Color(0xFFD700));
        g.setStroke(new BasicStroke(3f));
        if (selectedFrom == -1) {
            Rectangle bar = barRect();
            g.drawRect(bar.x + 2, bar.y + 2, bar.width - 4, bar.height - 4);
            return;
        }
        // find the board col/top for selectedFrom
        for (int col = 0; col < 12; col++) {
            for (boolean top : new boolean[]{true, false}) {
                if (colToIndex(col, top) == selectedFrom) {
                    Rectangle r = pointRect(col, top);
                    g.drawRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
                    return;
                }
            }
        }
    }

    private void drawStatusBar(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        int h = 22;
        g.fillRect(0, 0, getWidth(), h);
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        String turnName = state.turn == Player.WHITE ? state.whiteName + " (White)" : state.blackName + " (Black)";
        String dice = state.dice.isEmpty()
                ? "(no dice)"
                : state.dice.toString();
        g.drawString("Turn: " + turnName + "   Dice: " + dice
                + "   Off W:" + state.off[0] + " B:" + state.off[1]
                + "   Bar W:" + state.bar[0] + " B:" + state.bar[1],
                10, 16);
    }
}
