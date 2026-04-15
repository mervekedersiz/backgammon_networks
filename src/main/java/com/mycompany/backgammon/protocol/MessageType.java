package com.mycompany.backgammon.protocol;

public enum MessageType {
    // Client -> Server
    HELLO,          // payload: String playerName
    MOVE,           // payload: Move
    ROLL,           // payload: null (request to roll dice at start of turn)
    END_TURN,       // payload: null (player declares turn finished if no more moves)
    REPLAY,         // payload: null (ask to rematch after game ends)
    QUIT,           // payload: null

    // Server -> Client
    WAITING,        // payload: String message ("Waiting for opponent...")
    ASSIGN_COLOR,   // payload: Player (you are WHITE or BLACK)
    STATE,          // payload: GameState
    MESSAGE,        // payload: String (info / chat / status)
    ILLEGAL_MOVE,   // payload: String reason
    GAME_OVER       // payload: Player winner
}
