package com.mycompany.backgammon.client;

import com.mycompany.backgammon.game.BackgammonLogic;
import com.mycompany.backgammon.game.GameState;
import com.mycompany.backgammon.game.Move;
import com.mycompany.backgammon.game.Player;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Main game window: board panel + message log + dice / action bar.
 * Two-click move entry: click source point (or bar), then destination
 * (or tray for bearing off). The die is inferred from the move geometry.
 */
public class GameScreen extends JFrame {

    private final ClientConnection conn;

    private final BoardPanel board = new BoardPanel();
    private final DicePanel dicePanel = new DicePanel();
    private final JButton rollButton    = new JButton("Zar At");
    private final JButton undoButton    = new JButton("Geri Al");
    private final JButton endTurnButton = new JButton("End turn / Pass");
    private final JTextArea log = new JTextArea(10, 24);
    private final JLabel turnLabel = new JLabel(" ");
    private final JLabel diceLabel = new JLabel(" ");

    private volatile Player myColor; // null until assigned
    private volatile GameState lastState;
    private int pendingFrom = Integer.MIN_VALUE; // -2 none, -1 bar, else 0..23
    private Set<Integer> legalTargets = new LinkedHashSet<>();
    private boolean gameOverSeen = false;

    public GameScreen(ClientConnection conn, String playerName) {
        super("Backgammon — " + playerName);
        this.conn = conn;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 640);
        setLocationRelativeTo(null);

        buildUi();
        wireConnection();
        wireBoardClicks();

        // register & kick off
        conn.startReader();
        conn.hello(playerName);
        appendLog("Connected as " + playerName + ". Sending HELLO...");

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                conn.quit();
                dispose();
                System.exit(0);
            }
        });
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // top status bar
        JPanel top = new JPanel(new GridLayout(1, 2));
        turnLabel.setOpaque(true);
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        turnLabel.setHorizontalAlignment(SwingConstants.CENTER);
        turnLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        diceLabel.setFont(diceLabel.getFont().deriveFont(Font.BOLD, 14f));
        diceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(turnLabel);
        top.add(diceLabel);
        root.add(top, BorderLayout.NORTH);

        root.add(board, BorderLayout.CENTER);

        // right panel: dice + log + buttons
        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.setPreferredSize(new Dimension(260, 0));

        // dice area at top of right panel
        JPanel diceArea = new JPanel(new BorderLayout(4, 4));
        diceArea.setBorder(BorderFactory.createTitledBorder("Zar"));
        diceArea.add(dicePanel, BorderLayout.CENTER);
        rollButton.setFont(rollButton.getFont().deriveFont(Font.BOLD, 16f));
        rollButton.setEnabled(false);
        rollButton.addActionListener(e -> {
            rollButton.setEnabled(false);
            conn.rollDice();
        });
        diceArea.add(rollButton, BorderLayout.SOUTH);
        right.add(diceArea, BorderLayout.NORTH);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        right.add(new JScrollPane(log), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(4, 1, 4, 4));
        endTurnButton.addActionListener(e -> conn.endTurn());
        endTurnButton.setEnabled(false);
        JButton cancel = new JButton("Cancel selection");
        cancel.addActionListener(e -> clearPendingSelection());
        undoButton.setEnabled(false);
        undoButton.addActionListener(e -> conn.undo());
        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> { conn.quit(); dispose(); System.exit(0); });
        buttons.add(endTurnButton);
        buttons.add(undoButton);
        buttons.add(cancel);
        buttons.add(quit);
        right.add(buttons, BorderLayout.SOUTH);

        root.add(right, BorderLayout.EAST);
        setContentPane(root);
    }

    private void wireConnection() {
        conn.onAssignColor = color -> SwingUtilities.invokeLater(() -> {
            myColor = color;
            appendLog("You are " + color + ".");
        });
        conn.onWaiting = text -> SwingUtilities.invokeLater(() -> appendLog(text));
        conn.onMessage = text -> SwingUtilities.invokeLater(() -> appendLog(text));
        conn.onIllegalMove = text -> SwingUtilities.invokeLater(() -> {
            appendLog("[ILLEGAL] " + text);
            clearPendingSelection();
        });
        conn.onState = s -> SwingUtilities.invokeLater(() -> {
            if (gameOverSeen && s.off[0] == 0 && s.off[1] == 0
                    && s.die1 == 0 && s.die2 == 0 && s.needsRoll) {
                log.setText("");
                appendLog("Yeni oyun basladi!");
                gameOverSeen = false;
            }
            lastState = s;
            board.setState(s);
            clearPendingSelection();
            refreshLabels();
            updateDice();
        });
        conn.onGameOver = winner -> SwingUtilities.invokeLater(() -> {
            gameOverSeen = true;
            showGameOver(winner);
        });
        conn.onDisconnect = () -> SwingUtilities.invokeLater(() -> {
            appendLog("Disconnected from server.");
            JOptionPane.showMessageDialog(this, "Disconnected from server.",
                    "Disconnected", JOptionPane.WARNING_MESSAGE);
        });
    }

    private void wireBoardClicks() {
        board.setOnPointClicked(idx -> {
            if (!isMyTurn()) { appendLog("Not your turn."); return; }
            GameState s = lastState;
            if (s == null) return;

            if (pendingFrom == Integer.MIN_VALUE) {
                // First click: select source — must have own checker there
                if (s.bar[s.barIndex(myColor)] > 0) {
                    appendLog("You have checkers on the bar — click the bar to enter.");
                    return;
                }
                if (s.countAt(idx, myColor) == 0) {
                    appendLog("No own checker at point " + idx + ".");
                    return;
                }
                Set<Integer> targets = BackgammonLogic.legalDestinations(s, myColor, idx);
                if (targets.isEmpty()) return;
                setPendingSelection(idx, targets);
                appendLog("Selected point " + idx + ". Now click destination.");
            } else {
                // Second click: if same point clicked again, cancel selection
                if (idx == pendingFrom) {
                    clearPendingSelection();
                    appendLog("Selection cancelled.");
                    return;
                }
                if (!legalTargets.contains(idx)) return;
                attemptMove(pendingFrom, idx);
            }
        });
        board.setOnBarClicked(() -> {
            if (!isMyTurn()) return;
            GameState s = lastState;
            if (s == null) return;
            if (s.bar[s.barIndex(myColor)] == 0) {
                appendLog("No checkers on the bar.");
                return;
            }
            Set<Integer> targets = BackgammonLogic.legalDestinations(s, myColor, -1);
            if (targets.isEmpty()) return;
            setPendingSelection(-1, targets);
            appendLog("Bar selected. Click your entry point.");
        });
        board.setOnOffClicked(() -> {
            if (!isMyTurn()) return;
            if (pendingFrom == Integer.MIN_VALUE || !legalTargets.contains(-1)) return;
            attemptMove(pendingFrom, -1);
        });
    }

    private boolean isMyTurn() {
        return lastState != null && myColor != null && lastState.turn == myColor
                && lastState.winner == null && !lastState.needsRoll;
    }

    private void attemptMove(int from, int to) {
        if (!legalTargets.contains(to)) return;
        Integer die = pickDie(from, to);
        if (die == null) {
            clearPendingSelection();
            return;
        }
        conn.move(new Move(from, to, die));
        clearPendingSelection();
    }

    private void setPendingSelection(int from, Set<Integer> targets) {
        pendingFrom = from;
        legalTargets = new LinkedHashSet<>(targets);
        board.setSelected(from);
        board.setLegalTargets(legalTargets);
    }

    private void clearPendingSelection() {
        pendingFrom = Integer.MIN_VALUE;
        legalTargets.clear();
        board.clearSelection();
    }

    /** Pick a die value from remaining dice that is consistent with the requested move. */
    private Integer pickDie(int from, int to) {
        if (lastState == null) return null;
        Set<Integer> dice = new LinkedHashSet<>(lastState.dice);
        Player p = myColor;

        if (from == -1) {
            // bar entry
            for (int d : dice) {
                int entry = (p == Player.WHITE) ? 24 - d : d - 1;
                if (entry == to) return d;
            }
            return null;
        }
        if (to == -1) {
            // bearing off — prefer exact die, then smallest die >= required
            int need = (p == Player.WHITE) ? from + 1 : 24 - from;
            if (dice.contains(need)) return need;
            Integer bestLarger = null;
            for (int d : dice) {
                if (d > need && (bestLarger == null || d < bestLarger)) bestLarger = d;
            }
            return bestLarger;
        }
        int diff = (p == Player.WHITE) ? (from - to) : (to - from);
        if (diff <= 0) return null;
        return dice.contains(diff) ? diff : null;
    }

    private void updateDice() {
        if (lastState == null) return;
        boolean myTurn = myColor != null && lastState.turn == myColor && lastState.winner == null;
        rollButton.setEnabled(myTurn && lastState.needsRoll);
        undoButton.setEnabled(myTurn && !lastState.needsRoll);
        boolean canEnd = myTurn && !lastState.needsRoll
                && !BackgammonLogic.hasAnyMove(lastState, myColor);
        endTurnButton.setEnabled(canEnd);
        if (!lastState.dice.isEmpty()) {
            dicePanel.setRemainingDice(lastState.dice);
        } else {
            dicePanel.clearDice();
        }
    }

    private void refreshLabels() {
        if (lastState == null) return;
        Player t = lastState.turn;
        String turnName = (t == Player.WHITE
                ? lastState.whiteName + " (Beyaz)"
                : lastState.blackName + " (Siyah)");

        if (lastState.winner != null) {
            String winnerName = (lastState.winner == Player.WHITE
                    ? lastState.whiteName + " (Beyaz)"
                    : lastState.blackName + " (Siyah)");
            turnLabel.setText("Oyun Bitti — Kazanan: " + winnerName);
            turnLabel.setBackground(new Color(0x3D2313));
            turnLabel.setForeground(Color.WHITE);
        } else if (myColor != null && t == myColor) {
            turnLabel.setText("Sira Sende!  —  " + turnName);
            turnLabel.setBackground(new Color(46, 125, 50));
            turnLabel.setForeground(Color.WHITE);
        } else {
            String oppName = (t == Player.WHITE
                    ? lastState.whiteName : lastState.blackName);
            turnLabel.setText("Rakibin Oynuyor...  —  " + oppName);
            turnLabel.setBackground(new Color(230, 120, 0));
            turnLabel.setForeground(Color.WHITE);
        }

        diceLabel.setText("Kalan Zar: " + lastState.dice);
    }

    private void appendLog(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void showGameOver(Player winner) {
        GameOverDialog dialog = new GameOverDialog(this, winner, myColor, lastState);
        dialog.setVisible(true);
        if (dialog.wantsReplay()) {
            conn.replay();
            appendLog("Tekrar oynama istendi, rakip bekleniyor...");
        } else {
            conn.quit();
            dispose();
            System.exit(0);
        }
    }
}
