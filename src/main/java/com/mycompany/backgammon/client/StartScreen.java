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

    private Timer dotTimer;
    private int dotCount = 0;

    public StartScreen() {
        super("Tavla — Baglanti");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Gradient background panel
        JPanel root = new JPanel(new BorderLayout(12, 12)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0xFFF3DC),
                        0, getHeight(), new Color(0xC8A063)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Title
        JLabel title = new JLabel("TAVLA", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 44));
        title.setForeground(new Color(0x5A2200));
        root.add(title, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("SansSerif", Font.BOLD, 13);

        c.gridx = 0; c.gridy = 0; form.add(styledLabel("Server IP:", labelFont), c);
        c.gridx = 1;              form.add(hostField, c);
        c.gridx = 0; c.gridy = 1; form.add(styledLabel("Port:", labelFont), c);
        c.gridx = 1;              form.add(portField, c);
        c.gridx = 0; c.gridy = 2; form.add(styledLabel("Adiniz:", labelFont), c);
        c.gridx = 1;              form.add(nameField, c);

        root.add(form, BorderLayout.CENTER);

        // South: status + buttons
        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setOpaque(false);

        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        south.add(statusLabel, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);

        JButton connectBtn = styledButton("Baglan & Oyna", new Color(0x5A2200));
        connectBtn.addActionListener(e -> onConnect());
        getRootPane().setDefaultButton(connectBtn);

        JButton rulesBtn = styledButton("Oyun Kurallari", new Color(0x3D5A00));
        rulesBtn.addActionListener(e -> showRules());

        btnRow.add(connectBtn);
        btnRow.add(rulesBtn);
        south.add(btnRow, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(380, 300));
        setLocationRelativeTo(null);
    }

    private JLabel styledLabel(String text, Font font) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(new Color(0x3D2313));
        return l;
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void onConnect() {
        String host = hostField.getText().trim();
        String name = nameField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            showStatus("Gecersiz port numarasi", Color.RED);
            return;
        }
        if (host.isEmpty() || name.isEmpty()) {
            showStatus("IP ve isim zorunlu", Color.RED);
            return;
        }

        startConnectingAnimation(host, port);

        new Thread(() -> {
            try {
                ClientConnection conn = new ClientConnection(host, port);
                SwingUtilities.invokeLater(() -> {
                    stopAnimation();
                    GameScreen screen = new GameScreen(conn, name);
                    screen.setVisible(true);
                    dispose();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    stopAnimation();
                    showStatus("Baglanti basarisiz: " + ex.getMessage(), Color.RED);
                });
            }
        }, "connect-thread").start();
    }

    private void startConnectingAnimation(String host, int port) {
        dotCount = 0;
        dotTimer = new Timer(500, e -> {
            dotCount = (dotCount + 1) % 4;
            String dots = ".".repeat(dotCount);
            statusLabel.setForeground(new Color(0x204A0A));
            statusLabel.setText("Baglaniliyor" + dots + " (" + host + ":" + port + ")");
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
            "TAVLA KURALLARI\n\n" +
            "Amac: 15 tasin tamamini ev bolgesine tasiyip toplamak.\n\n" +
            "Hareket:\n" +
            "  - Beyaz: 23 -> 0 yonunde ilerler (ev: 0-5)\n" +
            "  - Siyah: 0 -> 23 yonunde ilerler (ev: 18-23)\n\n" +
            "Bar:\n" +
            "  - Rakibin tek tasi bulunan noktaya gidilince o tas bara duser.\n" +
            "  - Barda tas varken once bara giris yapilmali.\n\n" +
            "Blok:\n" +
            "  - Rakibin 2+ tasi olan noktaya gidilemez.\n\n" +
            "Toplama (Bearing Off):\n" +
            "  - Tum taslarin ev bolgesinde olmasi gerekir.\n" +
            "  - Sag taraftaki tray alanina tikla.\n\n" +
            "Cift Zar:\n" +
            "  - Cift atilinca 4 hamle hakki kazanilir.\n\n" +
            "Geri Al:\n" +
            "  - 'Geri Al' butonu ile o turdaki son hamle iptal edilebilir.";

        JTextArea area = new JTextArea(rules);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(400, 320));

        JOptionPane.showMessageDialog(this, scroll, "Oyun Kurallari", JOptionPane.PLAIN_MESSAGE);
    }
}
