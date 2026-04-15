package com.mycompany.backgammon.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of a backgammon game.
 * Board convention:
 *   points[0..23] — positive count = WHITE checkers, negative count = BLACK checkers.
 *   WHITE moves 23 -> 0, bears off from 0..5.
 *   BLACK moves  0 -> 23, bears off from 18..23.
 *   bar[0] = WHITE on bar, bar[1] = BLACK on bar.
 *   off[0] = WHITE borne off,  off[1] = BLACK borne off.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int[] points = new int[24];
    public final int[] bar = new int[2];
    public final int[] off = new int[2];

    public Player turn = Player.WHITE;
    // Remaining dice values for the current turn (doubles expand to 4 entries).
    public final List<Integer> dice = new ArrayList<>();
    public int die1 = 0;
    public int die2 = 0;

    public Player winner = null;
    public String whiteName = "White";
    public String blackName = "Black";

    public GameState() {
        reset();
    }

    public final void reset() {
        for (int i = 0; i < 24; i++) points[i] = 0;
        // White starting (positive)
        points[23] = 2;
        points[12] = 5;
        points[7]  = 3;
        points[5]  = 5;
        // Black starting (negative)
        points[0]  = -2;
        points[11] = -5;
        points[16] = -3;
        points[18] = -5;

        bar[0] = 0; bar[1] = 0;
        off[0] = 0; off[1] = 0;
        turn = Player.WHITE;
        dice.clear();
        die1 = 0; die2 = 0;
        winner = null;
    }

    public int barIndex(Player p) { return p == Player.WHITE ? 0 : 1; }

    /** Count of player's checkers on a point (always non-negative). */
    public int countAt(int idx, Player p) {
        int v = points[idx];
        return (p == Player.WHITE) ? Math.max(v, 0) : Math.max(-v, 0);
    }

    /** Does the opponent own the point (2 or more)? */
    public boolean blockedBy(int idx, Player opp) {
        return countAt(idx, opp) >= 2;
    }

    /** Is there a single opponent checker (a blot) at the point? */
    public boolean blotOf(int idx, Player opp) {
        return countAt(idx, opp) == 1;
    }
}
