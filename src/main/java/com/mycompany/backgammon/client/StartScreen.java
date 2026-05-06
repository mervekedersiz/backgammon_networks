package com.mycompany.backgammon.client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * Start screen — collects player name, connects to the server, and launches GameScreen.
 * Server host/port use defaults (16.171.174.37:5000) and are not exposed in the UI.
 */
public class StartScreen extends JFrame {

    private static final String DEFAULT_HOST = "16.171.174.37";
    private static final int    DEFAULT_PORT  = 5000;

    private final JTextField nameField  = new JTextField(System.getProperty("user.name", "Player"), 14);
    private final JLabel     statusLabel = new JLabel(" ");

    private Timer dotTimer;
    private int   dotCount = 0;

    public StartScreen() {
        super("Backgammon");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0xFFF3DC),
                        0, getHeight(), new Color(0xC8A063)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);
        // ---- TOP: splash image (falls back to text title) ----
        JComponent header = buildHeader();
        root.add(header, BorderLayout.NORTH);

        // Name form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(12, 36, 4, 36));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 8, 10, 8);
        c.anchor = GridBagConstraints.WEST;

        JLabel nameLabel = new JLabel("Your Name:");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(new Color(0x3D2313));

        nameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x8B5A2B), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        c.gridx = 0; c.gridy = 0; form.add(nameLabel, c);
        c.gridx = 1;              form.add(nameField, c);
        root.add(form, BorderLayout.CENTER);

        // South: status + buttons
        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 36, 20, 36));

        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        south.add(statusLabel, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setOpaque(false);

        JButton connectBtn = styledButton("Connect & Play", new Color(0x5A2200));
        connectBtn.addActionListener(e -> onConnect());
        getRootPane().setDefaultButton(connectBtn);

        JButton rulesBtn = styledButton("Game Rules", new Color(0x3D5A00));
        rulesBtn.addActionListener(e -> showRules());

        btnRow.add(connectBtn);
        btnRow.add(rulesBtn);
        south.add(btnRow, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(360, 300));
        setLocationRelativeTo(null);
    }

    /** Tries to load bg.jpg; falls back to a text label if missing. */
    private JComponent buildHeader() {
        URL imgUrl = StartScreen.class.getResource("/images/bg.png");
        if (imgUrl != null) {
            try {
                BufferedImage raw = ImageIO.read(imgUrl);
                // Scale to panel width (340px) keeping aspect ratio
                int targetW = 340;
                int targetH = raw.getHeight() * targetW / raw.getWidth();
                Image scaled = raw.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                JLabel imgLabel = new JLabel(new ImageIcon(scaled));
                imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                return imgLabel;
            } catch (IOException ignored) { /* fall through */ }
        }
        // Fallback: text title
        JLabel title = new JLabel("BACKGAMMON", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 44));
        title.setForeground(new Color(0x5A2200));
        title.setBorder(BorderFactory.createEmptyBorder(24, 0, 8, 0));
        return title;
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void onConnect() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showStatus("Name is required.", Color.RED);
            return;
        }

        startConnectingAnimation();

        new Thread(() -> {
            try {
                ClientConnection conn = new ClientConnection(DEFAULT_HOST, DEFAULT_PORT);
                SwingUtilities.invokeLater(() -> {
                    stopAnimation();
                    GameScreen screen = new GameScreen(conn, name);
                    screen.setVisible(true);
                    dispose();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    stopAnimation();
                    showStatus("Connection failed: " + ex.getMessage(), Color.RED);
                });
            }
        }, "connect-thread").start();
    }

    private void startConnectingAnimation() {
        dotCount = 0;
        dotTimer = new Timer(500, e -> {
            dotCount = (dotCount + 1) % 4;
            statusLabel.setForeground(new Color(0x204A0A));
            statusLabel.setText("Connecting" + ".".repeat(dotCount));
        });
        dotTimer.start();
    }

    private void stopAnimation() {
        if (dotTimer != null) { dotTimer.stop(); dotTimer = null; }
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(msg);
    }

    private void showRules() {
        String rules =
            "BACKGAMMON RULES\n\n" +
            "Goal: Move all 15 checkers into your home board and bear them off.\n\n" +
            "Movement:\n" +
            "  - White moves 23 -> 0  (home: points 0-5)\n" +
            "  - Black moves  0 -> 23 (home: points 18-23)\n\n" +
            "Hit & Bar:\n" +
            "  - Landing on a single opponent checker sends it to the bar.\n" +
            "  - You must re-enter all bar checkers before making other moves.\n\n" +
            "Blocking:\n" +
            "  - A point with 2+ opponent checkers is blocked.\n\n" +
            "Bearing Off:\n" +
            "  - All your checkers must be in your home board first.\n" +
            "  - Click the tray area on the right to bear off.\n\n" +
            "Doubles:\n" +
            "  - Rolling doubles gives 4 moves instead of 2.\n\n" +
            "Undo:\n" +
            "  - The 'Undo' button cancels your last move within the same turn.";

        JTextArea area = new JTextArea(rules);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(420, 320));

        JOptionPane.showMessageDialog(this, scroll, "Game Rules", JOptionPane.PLAIN_MESSAGE);
    }
}
