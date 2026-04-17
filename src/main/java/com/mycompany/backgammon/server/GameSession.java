package com.mycompany.backgammon.server;

import com.mycompany.backgammon.game.BackgammonLogic;
import com.mycompany.backgammon.game.GameState;
import com.mycompany.backgammon.game.Move;
import com.mycompany.backgammon.game.Player;
import com.mycompany.backgammon.protocol.Message;
import com.mycompany.backgammon.protocol.MessageType;

/**
 * Two-client backgammon session: owns the GameState and drives the turn loop.
 */
public class GameSession implements Runnable {

    private final ClientHandler white;
    private final ClientHandler black;
    private final GameState state = new GameState();

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
                playGame();
                if (!bothAlive()) break;
                if (!askReplay()) break;
            } while (true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            white.close();
            black.close();
        }
    }

    private boolean bothAlive() { return white.isAlive() && black.isAlive(); }

    private void playGame() throws InterruptedException {
        state.needsRoll = true;
        broadcastState();

        int consecutivePasses = 0;

        while (state.winner == null && bothAlive()) {
            ClientHandler active = of(state.turn);

            if (state.needsRoll) {
                Message msg = active.takeBlocking();
                if (msg == null || msg.type == MessageType.QUIT) {
                    broadcastMsg(active.getName() + " quit"); return;
                }
                if (msg.type == MessageType.ROLL) {
                    int[] r = BackgammonLogic.rollDice();
                    BackgammonLogic.setDice(state, r[0], r[1]);
                    state.needsRoll = false;
                    broadcastMsg(active.getName() + " rolls " + r[0] + " & " + r[1]);

                    if (!BackgammonLogic.hasAnyMove(state, state.turn)) {
                        broadcastMsg(active.getName() + " has no legal moves — turn passes");
                        endTurn();
                        consecutivePasses++;
                        if (consecutivePasses >= 10) {
                            broadcastMsg("[Server] Too many consecutive passes — game aborted.");
                            return;
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
                broadcastMsg(active.getName() + " quit"); return;
            }
            switch (msg.type) {
                case MOVE -> handleMove(active, (Move) msg.payload);
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
    }

    private void handleMove(ClientHandler active, Move m) {
        String err = BackgammonLogic.applyMove(state, active.getColor(), m);
        if (err != null) {
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

    private void endTurn() {
        state.turn = state.turn.opponent();
        state.needsRoll = true;
        state.dice.clear();
        state.die1 = 0;
        state.die2 = 0;
        broadcastState();
    }

    /** Prompt both clients for replay. Returns true iff both confirm. */
    private boolean askReplay() throws InterruptedException {
        white.send(MessageType.MESSAGE, "Send REPLAY to play again, or QUIT to leave.");
        black.send(MessageType.MESSAGE, "Send REPLAY to play again, or QUIT to leave.");
        boolean wr = waitReplay(white);
        boolean br = waitReplay(black);
        return wr && br && bothAlive();
    }

    private boolean waitReplay(ClientHandler c) throws InterruptedException {
        while (c.isAlive()) {
            Message m = c.takeBlocking();
            if (m == null) return false;
            if (m.type == MessageType.REPLAY) return true;
            if (m.type == MessageType.QUIT) return false;
            // ignore stray messages
        }
        return false;
    }
}
