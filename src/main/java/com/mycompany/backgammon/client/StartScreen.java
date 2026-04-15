package com.mycompany.backgammon.client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Start screen — collects server IP / port / player name, connects, and
 * launches the GameScreen on success.
 */
public class StartScreen extends JFrame {

    private final JTextField hostField = new JTextField("127.0.0.1", 16);
    private final JTextField portField = new JTextField("5555", 6);
    private final JTextField nameField = new JTextField(System.getProperty("user.name", "Player"), 12);
    private final JLabel statusLabel = new JLabel(" ");

    public StartScreen() {
        super("Backgammon — Connect");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("BACKGAMMON", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(new Color(0x8B4513));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Server IP:"), c);
        c.gridx = 1;              form.add(hostField, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Port:"), c);
        c.gridx = 1;              form.add(portField, c);
        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Your name:"), c);
        c.gridx = 1;              form.add(nameField, c);

        root.add(form, BorderLayout.CENTER);

        JButton connectBtn = new JButton("Connect & Play");
        connectBtn.addActionListener(e -> onConnect());
        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.NORTH);
        south.add(connectBtn, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(connectBtn);
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void onConnect() {
        String host = hostField.getText().trim();
        String name = nameField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Invalid port");
            return;
        }
        if (host.isEmpty() || name.isEmpty()) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Host and name are required");
            return;
        }

        statusLabel.setForeground(new Color(0x204A0A));
        statusLabel.setText("Connecting to " + host + ":" + port + "...");

        new Thread(() -> {
            try {
                ClientConnection conn = new ClientConnection(host, port);
                SwingUtilities.invokeLater(() -> {
                    GameScreen screen = new GameScreen(conn, name);
                    screen.setVisible(true);
                    dispose();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Connection failed: " + ex.getMessage());
                });
            }
        }, "connect-thread").start();
    }
}
