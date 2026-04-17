package com.mycompany.backgammon.client;

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
    private final JButton rollButton = new JButton("Zar At");
    private final JTextArea log = new JTextArea(10, 24);
    private final JLabel turnLabel = new JLabel(" ");
    private final JLabel diceLabel = new JLabel(" ");

    private volatile Player myColor; // null until assigned
    private volatile GameState lastState;
    private int pendingFrom = Integer.MIN_VALUE; // -2 none, -1 bar, else 0..23

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
        top.add(turnLabel);
        top.add(diceLabel);
        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 14f));
        diceLabel.setFont(diceLabel.getFont().deriveFont(Font.BOLD, 14f));
        diceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
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

        JPanel buttons = new JPanel(new GridLayout(3, 1, 4, 4));
        JButton endTurn = new JButton("End turn / Pass");
        endTurn.addActionListener(e -> conn.endTurn());
        JButton cancel = new JButton("Cancel selection");
        cancel.addActionListener(e -> { pendingFrom = Integer.MIN_VALUE; board.clearSelection(); });
        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> { conn.quit(); dispose(); System.exit(0); });
        buttons.add(endTurn);
        buttons.add(cancel);
        buttons.add(quit);
        right.add(buttons, BorderLayout.SOUTH);

        root.add(right, BorderLayout.EAST);
        setContentPane(root);
    }

    private void wireConnection() {
        conn.onAssignColor = color -> SwingUtilities.invokeLater(() -> {
            myColor = color;
            board.setViewer(color);
            appendLog("You are " + color + ".");
        });
        conn.onWaiting = text -> SwingUtilities.invokeLater(() -> appendLog(text));
        conn.onMessage = text -> SwingUtilities.invokeLater(() -> appendLog(text));
        conn.onIllegalMove = text -> SwingUtilities.invokeLater(() -> {
            appendLog("[ILLEGAL] " + text);
            pendingFrom = Integer.MIN_VALUE;
            board.clearSelection();
        });
        conn.onState = s -> SwingUtilities.invokeLater(() -> {
            lastState = s;
            board.setState(s);
            pendingFrom = Integer.MIN_VALUE;
            board.clearSelection();
            refreshLabels();
            updateDice();
        });
        conn.onGameOver = winner -> SwingUtilities.invokeLater(() -> showGameOver(winner));
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
                pendingFrom = idx;
                board.setSelected(idx);
                appendLog("Selected point " + idx + ". Now click destination.");
            } else {
                // Second click: if same point clicked again, cancel selection
                if (idx == pendingFrom) {
                    pendingFrom = Integer.MIN_VALUE;
                    board.clearSelection();
                    appendLog("Selection cancelled.");
                    return;
                }
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
            pendingFrom = -1;
            board.setSelected(-1);
            appendLog("Bar selected. Click your entry point.");
        });
        board.setOnOffClicked(() -> {
            if (!isMyTurn()) return;
            if (pendingFrom == Integer.MIN_VALUE) {
                appendLog("Select a source point first.");
                return;
            }
            attemptMove(pendingFrom, -1);
        });
    }

    private boolean isMyTurn() {
        return lastState != null && myColor != null && lastState.turn == myColor
                && lastState.winner == null && !lastState.needsRoll;
    }

    private void attemptMove(int from, int to) {
        Integer die = pickDie(from, to);
        if (die == null) {
            appendLog("No remaining die fits this move.");
            pendingFrom = Integer.MIN_VALUE;
            board.clearSelection();
            return;
        }
        conn.move(new Move(from, to, die));
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
        if (lastState.die1 > 0 && lastState.die2 > 0) {
            dicePanel.setDice(lastState.die1, lastState.die2);
        } else {
            dicePanel.clearDice();
        }
    }

    private void refreshLabels() {
        if (lastState == null) return;
        Player t = lastState.turn;
        String turnName = (t == Player.WHITE ? lastState.whiteName + " (White)" : lastState.blackName + " (Black)");
        turnLabel.setText("Turn: " + turnName + (t == myColor ? "  <-- YOU" : ""));
        diceLabel.setText("Dice remaining: " + lastState.dice);
    }

    private void appendLog(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void showGameOver(Player winner) {
        String winnerName = (winner == Player.WHITE) ? (lastState != null ? lastState.whiteName : "White")
                                                    : (lastState != null ? lastState.blackName : "Black");
        String title = (winner == myColor) ? "You win!" : "You lose";
        int choice = JOptionPane.showConfirmDialog(this,
                winnerName + " wins!\n\nPlay again?",
                title,
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            conn.replay();
            appendLog("Replay requested, waiting for opponent...");
        } else {
            conn.quit();
            dispose();
            System.exit(0);
        }
    }
}
