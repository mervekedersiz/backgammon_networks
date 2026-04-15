package com.mycompany.backgammon.game;

import java.util.Random;

/**
 * Backgammon rule engine — stateless helpers operating on a GameState.
 * Board is indexed 0..23; WHITE moves 23->0, BLACK moves 0->23.
 */
public final class BackgammonLogic {

    private static final Random RNG = new Random();

    private BackgammonLogic() {}

    /* --------------------- dice --------------------- */

    public static int[] rollDice() {
        return new int[]{1 + RNG.nextInt(6), 1 + RNG.nextInt(6)};
    }

    /** Populate the dice list from two raw values (doubles expand to 4). */
    public static void setDice(GameState s, int d1, int d2) {
        s.die1 = d1; s.die2 = d2;
        s.dice.clear();
        if (d1 == d2) {
            for (int i = 0; i < 4; i++) s.dice.add(d1);
        } else {
            s.dice.add(d1);
            s.dice.add(d2);
        }
    }

    /* --------------------- queries --------------------- */

    public static boolean allInHome(GameState s, Player p) {
        if (s.bar[s.barIndex(p)] > 0) return false;
        if (p == Player.WHITE) {
            for (int i = 6; i < 24; i++) if (s.countAt(i, p) > 0) return false;
        } else {
            for (int i = 0; i < 18; i++) if (s.countAt(i, p) > 0) return false;
        }
        return true;
    }

    /** Is there any legal play for player p using die value d? */
    public static boolean hasAnyLegalMoveWithDie(GameState s, Player p, int die) {
        int bi = s.barIndex(p);
        if (s.bar[bi] > 0) {
            int entry = (p == Player.WHITE) ? 24 - die : die - 1;
            return entry >= 0 && entry <= 23 && !s.blockedBy(entry, p.opponent());
        }
        for (int i = 0; i < 24; i++) {
            if (s.countAt(i, p) > 0 && canMoveFrom(s, p, i, die)) return true;
        }
        return false;
    }

    /** Any legal move available among currently-remaining dice. */
    public static boolean hasAnyMove(GameState s, Player p) {
        for (int d : s.dice) if (hasAnyLegalMoveWithDie(s, p, d)) return true;
        return false;
    }

    private static boolean canMoveFrom(GameState s, Player p, int from, int die) {
        int dest = (p == Player.WHITE) ? from - die : from + die;
        if (dest >= 0 && dest <= 23) {
            return !s.blockedBy(dest, p.opponent());
        }
        // off-board: bearing-off attempt
        if (!allInHome(s, p)) return false;
        if (p == Player.WHITE) {
            if (die == from + 1) return true;
            if (die > from + 1) {
                for (int i = from + 1; i <= 5; i++) if (s.countAt(i, p) > 0) return false;
                return true;
            }
            return false;
        } else {
            if (die == 24 - from) return true;
            if (die > 24 - from) {
                for (int i = from - 1; i >= 18; i--) if (s.countAt(i, p) > 0) return false;
                return true;
            }
            return false;
        }
    }

    /* --------------------- move application --------------------- */

    /** Validate and apply a move. Returns null on success, or an error string. */
    public static String applyMove(GameState s, Player p, Move m) {
        if (s.turn != p) return "Not your turn";
        if (s.winner != null) return "Game is already over";
        if (!s.dice.contains(m.die)) return "Die value " + m.die + " is not available";

        int bi = s.barIndex(p);

        if (s.bar[bi] > 0) {
            if (m.from != -1) return "You must enter from the bar first";
            int entry = (p == Player.WHITE) ? 24 - m.die : m.die - 1;
            if (m.to != entry) return "Bar entry with die " + m.die + " must land on " + entry;
            if (s.blockedBy(entry, p.opponent())) return "Entry point " + entry + " is blocked";
            s.bar[bi]--;
            hitIfBlot(s, entry, p);
            placeAt(s, entry, p);
        } else if (m.from == -1) {
            return "You have no checkers on the bar";
        } else {
            if (m.from < 0 || m.from > 23) return "Invalid source point";
            if (s.countAt(m.from, p) == 0) return "No own checker at " + m.from;

            int dest = (p == Player.WHITE) ? m.from - m.die : m.from + m.die;

            if (m.to == -1) {
                // bearing off
                if (!allInHome(s, p)) return "Cannot bear off: not all checkers are in home";
                if (p == Player.WHITE) {
                    if (m.die == m.from + 1) {
                        // exact
                    } else if (m.die > m.from + 1) {
                        for (int i = m.from + 1; i <= 5; i++) {
                            if (s.countAt(i, p) > 0) {
                                return "Cannot bear off from " + m.from + " — higher checker exists";
                            }
                        }
                    } else {
                        return "Die too small to bear off from " + m.from;
                    }
                } else {
                    if (m.die == 24 - m.from) {
                        // exact
                    } else if (m.die > 24 - m.from) {
                        for (int i = m.from - 1; i >= 18; i--) {
                            if (s.countAt(i, p) > 0) {
                                return "Cannot bear off from " + m.from + " — higher checker exists";
                            }
                        }
                    } else {
                        return "Die too small to bear off from " + m.from;
                    }
                }
                removeFrom(s, m.from, p);
                s.off[bi]++;
            } else {
                if (m.to != dest) return "Destination must be " + dest + " for die " + m.die;
                if (dest < 0 || dest > 23) return "Destination off the board — use bear off";
                if (s.blockedBy(dest, p.opponent())) return "Destination " + dest + " is blocked";
                removeFrom(s, m.from, p);
                hitIfBlot(s, dest, p);
                placeAt(s, dest, p);
            }
        }

        // consume die (one occurrence)
        s.dice.remove(Integer.valueOf(m.die));

        if (s.off[bi] >= 15) {
            s.winner = p;
        }
        return null;
    }

    /* --------------------- low-level helpers --------------------- */

    private static void placeAt(GameState s, int idx, Player p) {
        s.points[idx] += (p == Player.WHITE) ? 1 : -1;
    }

    private static void removeFrom(GameState s, int idx, Player p) {
        s.points[idx] -= (p == Player.WHITE) ? 1 : -1;
    }

    private static void hitIfBlot(GameState s, int idx, Player p) {
        Player opp = p.opponent();
        if (s.blotOf(idx, opp)) {
            // remove the opponent's single checker and send to bar
            s.points[idx] -= (opp == Player.WHITE) ? 1 : -1;
            s.bar[s.barIndex(opp)]++;
        }
    }
}
