package com.mycompany.backgammon.client;

import com.mycompany.backgammon.game.GameState;
import com.mycompany.backgammon.game.Move;
import com.mycompany.backgammon.game.Player;
import com.mycompany.backgammon.protocol.Message;
import com.mycompany.backgammon.protocol.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP connection to the backgammon server. Reader thread dispatches typed
 * callbacks; writers are synchronized for safety.
 */
public class ClientConnection {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile boolean running = true;

    public Consumer<Player>   onAssignColor = p -> {};
    public Consumer<GameState> onState      = s -> {};
    public Consumer<String>   onMessage     = m -> {};
    public Consumer<String>   onIllegalMove = m -> {};
    public Consumer<Player>   onGameOver    = w -> {};
    public Consumer<String>   onWaiting     = m -> {};
    public Runnable           onDisconnect  = () -> {};

    public ClientConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /** Must be called after all callbacks are wired. */
    public void startReader() {
        Thread t = new Thread(this::readerLoop, "server-reader");
        t.setDaemon(true);
        t.start();
    }

    private void readerLoop() {
        try {
            while (running) {
                Object o = in.readObject();
                if (!(o instanceof Message msg)) continue;
                dispatch(msg);
            }
        } catch (Exception e) {
            // falls through to disconnect callback
        } finally {
            running = false;
            onDisconnect.run();
        }
    }

    private void dispatch(Message msg) {
        switch (msg.type) {
            case ASSIGN_COLOR -> onAssignColor.accept((Player) msg.payload);
            case STATE        -> onState.accept((GameState) msg.payload);
            case MESSAGE      -> onMessage.accept((String) msg.payload);
            case ILLEGAL_MOVE -> onIllegalMove.accept((String) msg.payload);
            case GAME_OVER    -> onGameOver.accept((Player) msg.payload);
            case WAITING      -> onWaiting.accept((String) msg.payload);
            default -> { /* client ignores other server messages */ }
        }
    }

    public synchronized void send(Message m) {
        try {
            out.writeObject(m);
            out.reset();
            out.flush();
        } catch (IOException e) {
            running = false;
            onDisconnect.run();
        }
    }

    public void hello(String name)        { send(new Message(MessageType.HELLO, name)); }
    public void rollDice()                { send(new Message(MessageType.ROLL)); }
    public void move(Move m)              { send(new Message(MessageType.MOVE, m)); }
    public void endTurn()                 { send(new Message(MessageType.END_TURN)); }
    public void replay()                  { send(new Message(MessageType.REPLAY)); }
    public void quit() {
        send(new Message(MessageType.QUIT));
        close();
    }

    public void close() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
