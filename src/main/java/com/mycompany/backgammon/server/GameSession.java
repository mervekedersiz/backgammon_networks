package com.mycompany.backgammon.server;

import com.mycompany.backgammon.game.BackgammonLogic;
import com.mycompany.backgammon.game.GameState;
import com.mycompany.backgammon.game.Move;
import com.mycompany.backgammon.game.Player;
import com.mycompany.backgammon.protocol.Message;
import com.mycompany.backgammon.protocol.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Two-client backgammon session: owns the GameState and drives the turn loop.
 */
public class GameSession implements Runnable {

    private final ClientHandler white;
    private final ClientHandler black;
    private final GameState state = new GameState();
    private volatile ClientHandler requeuedHandler = null;

    // Snapshots taken before each move in the current turn; cleared on new turn roll.
    private final List<GameState> turnSnapshots = new ArrayList<>();

    public GameSession(ClientHandler white, ClientHandler black) {
        this.white = white;
        this.black = black;
        white.setColor(Player.WHITE);
        black.setColor(Player.BLACK);
        state.whiteName = white.getName();
        state.blackName = black.getName();
    }

    private ClientHandler of(Player p) { return p == Player.WHITE ? white : black; }

    private void broadcastState() {
        white.send(MessageType.STATE, state);
        black.send(MessageType.STATE, state);
    }

    private void broadcastMsg(String s) {
        white.send(MessageType.MESSAGE, s);
        black.send(MessageType.MESSAGE, s);
    }

    @Override
    public void run() {
        try {
            white.send(MessageType.ASSIGN_COLOR, Player.WHITE);
            black.send(MessageType.ASSIGN_COLOR, Player.BLACK);
            broadcastMsg("Game started: " + white.getName() + " (White) vs " + black.getName() + " (Black)");

            do {
                state.reset();
                state.whiteName = white.getName();
                state.blackName = black.getName();
                state.turn = Player.WHITE;
                boolean quitOccurred = playGame();
                if (quitOccurred) break;  // opponent-quit flow already handled inside
                if (!bothAlive()) break;
                if (!askReplay()) break;
            } while (true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (white != requeuedHandler) white.close();
            if (black != requeuedHandler) black.close();
        }
    }

    private boolean bothAlive() { return white.isAlive() && black.isAlive(); }

    /**
     * Returns true if a player quit mid-game (opponent-quit flow was handled).
     * Returns false if game ended normally (winner determined).
     */
    private boolean playGame() throws InterruptedException {
        state.needsRoll = true;
        broadcastState();

        int consecutivePasses = 0;

        while (state.winner == null && bothAlive()) {
            ClientHandler active = of(state.turn);

            if (state.needsRoll) {
                Message msg = active.takeBlocking();
                if (msg == null || msg.type == MessageType.QUIT) {
                    handleMidGameQuit(active);
                    return true;
                }
                if (msg.type == MessageType.ROLL) {
                    int[] r = BackgammonLogic.rollDice();
                    BackgammonLogic.setDice(state, r[0], r[1]);
                    state.needsRoll = false;
                    turnSnapshots.clear(); // new turn: reset undo history
                    broadcastMsg(active.getName() + " rolls " + r[0] + " & " + r[1]);

                    if (!BackgammonLogic.hasAnyMove(state, state.turn)) {
                        broadcastMsg(active.getName() + " has no legal moves — turn passes");
                        endTurn();
                        consecutivePasses++;
                        if (consecutivePasses >= 10) {
                            broadcastMsg("[Server] Too many consecutive passes — game aborted.");
                            return false;
                        }
                        continue;
                    }
                    consecutivePasses = 0;
                    broadcastState();
                } else {
                    active.send(MessageType.MESSAGE, "You must roll the dice first.");
                }
                continue;
            }

            Message msg = active.takeBlocking();
            if (msg == null || msg.type == MessageType.QUIT) {
                handleMidGameQuit(active);
                return true;
            }
            switch (msg.type) {
                case MOVE -> handleMove(active, (Move) msg.payload);
                case UNDO -> handleUndo(active);
                case END_TURN -> {
                    if (BackgammonLogic.hasAnyMove(state, active.getColor())) {
                        active.send(MessageType.MESSAGE,
                                "You still have legal moves — you must play them.");
                    } else {
                        endTurn();
                    }
                }
                default -> active.send(MessageType.MESSAGE, "Unexpected message: " + msg.type);
            }
        }

        if (state.winner != null) {
            broadcastMsg("Winner: " + of(state.winner).getName());
            white.send(MessageType.GAME_OVER, state.winner);
            black.send(MessageType.GAME_OVER, state.winner);
        }
        return false;
    }

    /**
     * Called when a player quits mid-game. Notifies the remaining player
     * with OPPONENT_QUIT and waits for their replay/quit decision.
     */
    private void handleMidGameQuit(ClientHandler quitter) throws InterruptedException {
        ClientHandler remaining = (quitter == white) ? black : white;
        quitter.close();

        // Send OPPONENT_QUIT to the remaining player with the quitter's name
        remaining.send(MessageType.OPPONENT_QUIT, quitter.getName());

        // Wait for remaining player's response: REPLAY or QUIT
        long deadline = System.currentTimeMillis() + 60_000;
        while (remaining.isAlive()) {
            long rem = deadline - System.currentTimeMillis();
            if (rem <= 0) {
                remaining.send(MessageType.MESSAGE, "Zaman aşımı — bağlantı kapatılıyor.");
                remaining.close();
                return;
            }
            Message m = remaining.poll(rem);
            if (m == null) {
                remaining.close();
                return;
            }
            if (m.type == MessageType.QUIT) {
                remaining.close();
                return;
            }
            if (m.type == MessageType.REPLAY) {
                // Put remaining player back in the server's waiting queue
                remaining.send(MessageType.WAITING, "Rakip aranıyor...");
                requeuedHandler = remaining;
                BackgammonServer.getInstance().requeue(remaining);
                return;
            }
        }
    }

    private void handleMove(ClientHandler active, Move m) {
        // Save snapshot before applying so UNDO can restore it
        turnSnapshots.add(state.deepCopy());

        String err = BackgammonLogic.applyMove(state, active.getColor(), m);
        if (err != null) {
            turnSnapshots.remove(turnSnapshots.size() - 1); // rollback snapshot
            active.send(MessageType.ILLEGAL_MOVE, err);
            return;
        }
        if (state.winner == null) {
            if (state.dice.isEmpty() || !BackgammonLogic.hasAnyMove(state, state.turn)) {
                if (!state.dice.isEmpty()) {
                    broadcastMsg(active.getName() + " cannot use remaining dice");
                }
                endTurn();
                return;
            }
        }
        broadcastState();
    }

    private void handleUndo(ClientHandler active) {
        if (turnSnapshots.isEmpty()) {
            active.send(MessageType.MESSAGE, "Geri alinacak hamle yok.");
            return;
        }
        GameState prev = turnSnapshots.remove(turnSnapshots.size() - 1);
        restoreState(prev);
        broadcastState();
        broadcastMsg(active.getName() + " son hamlesini geri aldi.");
    }

    private void restoreState(GameState snap) {
        System.arraycopy(snap.points, 0, state.points, 0, 24);
        System.arraycopy(snap.bar, 0, state.bar, 0, 2);
        System.arraycopy(snap.off, 0, state.off, 0, 2);
        state.turn = snap.turn;
        state.dice.clear();
        state.dice.addAll(snap.dice);
        state.die1 = snap.die1;
        state.die2 = snap.die2;
        state.needsRoll = snap.needsRoll;
        state.winner = snap.winner;
    }

    private void endTurn() {
        state.turn = state.turn.opponent();
        state.needsRoll = true;
        state.dice.clear();
        state.die1 = 0;
        state.die2 = 0;
        broadcastState();
    }

    /** Prompt both clients for replay in parallel. Returns true iff both confirm. */
    private boolean askReplay() throws InterruptedException {
        white.send(MessageType.MESSAGE, "Oyun bitti! REPLAY veya QUIT gonderin.");
        black.send(MessageType.MESSAGE, "Oyun bitti! REPLAY veya QUIT gonderin.");

        boolean[] results = new boolean[2];
        Thread wt = new Thread(() -> {
            try { results[0] = waitReplay(white); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "replay-wait-white");
        Thread bt = new Thread(() -> {
            try { results[1] = waitReplay(black); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "replay-wait-black");
        wt.start();
        bt.start();
        wt.join();
        bt.join();

        if (results[0] && !results[1]) white.send(MessageType.MESSAGE, "Rakibiniz tekrar oynamak istemedi.");
        if (!results[0] && results[1]) black.send(MessageType.MESSAGE, "Rakibiniz tekrar oynamak istemedi.");

        return results[0] && results[1] && bothAlive();
    }

    private boolean waitReplay(ClientHandler c) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 60_000;
        while (c.isAlive()) {
            long rem = deadline - System.currentTimeMillis();
            if (rem <= 0) return false;
            Message m = c.poll(rem);
            if (m == null) return false;
            if (m.type == MessageType.REPLAY) return true;
            if (m.type == MessageType.QUIT) return false;
        }
        return false;
    }
}
