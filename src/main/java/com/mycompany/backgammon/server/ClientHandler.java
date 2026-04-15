package com.mycompany.backgammon.server;

import com.mycompany.backgammon.game.Player;
import com.mycompany.backgammon.protocol.Message;
import com.mycompany.backgammon.protocol.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * One per connected client: owns the socket I/O streams, pushes incoming
 * messages onto a queue for the GameSession to consume.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final BlockingQueue<Message> incoming = new LinkedBlockingQueue<>();
    private volatile boolean alive = true;

    private String name = "Player";
    private Player color; // assigned by session

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        // IMPORTANT: create ObjectOutputStream first to avoid deadlock on header exchange.
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Player getColor() { return color; }
    public void setColor(Player color) { this.color = color; }
    public boolean isAlive() { return alive; }

    /** Blocking take with timeout — returns null if nothing arrived in time. */
    public Message poll(long timeoutMs) throws InterruptedException {
        return incoming.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public Message takeBlocking() throws InterruptedException {
        return incoming.take();
    }

    public synchronized void send(Message m) {
        if (!alive) return;
        try {
            out.writeObject(m);
            out.reset(); // avoid the "same object cached" problem across turns
            out.flush();
        } catch (IOException e) {
            System.err.println("send failed to " + name + ": " + e.getMessage());
            close();
        }
    }

    public void send(MessageType t, Object payload) {
        send(new Message(t, payload));
    }

    @Override
    public void run() {
        try {
            while (alive) {
                Object o = in.readObject();
                if (o instanceof Message msg) {
                    incoming.put(msg);
                }
            }
        } catch (Exception e) {
            // normal on disconnect
        } finally {
            close();
        }
    }

    public void close() {
        alive = false;
        try { socket.close(); } catch (IOException ignored) {}
        // push a sentinel so blocked session wakes up
        incoming.offer(new Message(MessageType.QUIT));
    }
}
