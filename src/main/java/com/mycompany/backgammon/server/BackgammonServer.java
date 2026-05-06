package com.mycompany.backgammon.server;

import com.mycompany.backgammon.protocol.Message;
import com.mycompany.backgammon.protocol.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Console-only backgammon server.
 *
 * Pairing rules (per project spec):
 *   - Two connected players are paired into a game session.
 *   - A third arriving player waits.
 *   - A fourth arriving player pairs with the third, so both waiting players
 *     start a new game together.
 *
 * Run with:  java -cp ... com.mycompany.backgammon.server.BackgammonServer [port]
 */
public class BackgammonServer {

    public static final int DEFAULT_PORT = 5000;

    private final int port;
    private final Deque<ClientHandler> waiting = new ArrayDeque<>();

    public BackgammonServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[Server] Listening on port " + port);
            System.out.println("[Server] Bind address: " + server.getInetAddress());
            while (true) {
                Socket socket = server.accept();
                System.out.println("[Server] Client connected: " + socket.getRemoteSocketAddress());
                handleNewConnection(socket);
            }
        }
    }

    private void handleNewConnection(Socket socket) {
        try {
            ClientHandler h = new ClientHandler(socket);
            // start reader thread immediately
            Thread t = new Thread(h, "client-reader");
            t.setDaemon(true);
            t.start();
            // expect HELLO within 30s
            Message hello = h.poll(30_000);
            if (hello == null || hello.type != MessageType.HELLO) {
                h.send(MessageType.MESSAGE, "Expected HELLO first — closing.");
                h.close();
                return;
            }
            h.setName(hello.payload == null ? "Player" : hello.payload.toString());
            System.out.println("[Server] " + h.getName() + " joined");
            enqueueAndPair(h);
        } catch (IOException | InterruptedException e) {
            System.err.println("[Server] connection setup error: " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    private void enqueueAndPair(ClientHandler h) {
        ClientHandler partner = null;
        synchronized (waiting) {
            // prune dead waiters
            waiting.removeIf(w -> !w.isAlive());
            if (!waiting.isEmpty()) {
                partner = waiting.pollFirst();
            } else {
                waiting.addLast(h);
                h.send(MessageType.WAITING, "Waiting for an opponent...");
                System.out.println("[Server] " + h.getName() + " is waiting");
            }
        }
        if (partner != null) {
            System.out.println("[Server] Pairing " + partner.getName() + " vs " + h.getName());
            GameSession session = new GameSession(partner, h);
            Thread t = new Thread(session, "session-" + partner.getName() + "-vs-" + h.getName());
            t.setDaemon(true);
            t.start();
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) {
                System.err.println("Invalid port, using default " + DEFAULT_PORT);
            }
        }
        try {
            new BackgammonServer(port).start();
        } catch (IOException e) {
            System.err.println("[Server] fatal: " + e.getMessage());
            System.exit(1);
        }
    }
}
