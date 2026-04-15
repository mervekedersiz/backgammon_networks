package com.mycompany.backgammon.game;

import java.io.Serializable;

/**
 * Single checker move. from = -1 means "from bar". to = -1 means "bear off".
 * die is the die value used (required — the server validates it).
 */
public class Move implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int from;
    public final int to;
    public final int die;

    public Move(int from, int to, int die) {
        this.from = from;
        this.to = to;
        this.die = die;
    }

    @Override
    public String toString() {
        return "Move(" + from + "->" + to + " die=" + die + ")";
    }
}
