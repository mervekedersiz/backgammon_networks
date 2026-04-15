package com.mycompany.backgammon.game;

public enum Player {
    WHITE, BLACK;

    public Player opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
