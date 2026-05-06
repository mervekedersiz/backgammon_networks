package com.mycompany.backgammon.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Client entry point. Shows the start screen; further screens are driven
 * from there
 */
public class BackgammonClient {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { /* keep default L&F on failure */ }

        SwingUtilities.invokeLater(() -> new StartScreen().setVisible(true));
    }
}
