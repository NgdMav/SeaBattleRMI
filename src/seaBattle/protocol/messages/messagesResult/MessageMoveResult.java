package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessageMoveResult extends MessageResult {

    private static final long serialVersionUID = 1L;

    private long sessionId;
    private int x;
    private int y;
    private boolean hitted;
    private boolean sunked;
    private boolean gameOver;
    private int[][] enemyField;
    private boolean enemy;

    public MessageMoveResult(boolean isGood, String message, long sessionId, int x, int y, boolean hitted,
            boolean sunked, boolean gameOver, int[][] enemyField, boolean enemy) {
        super(Protocol.CMD_MOVE, isGood, message);
        this.sessionId = sessionId;
        this.x = x;
        this.y = y;
        this.hitted = hitted;
        this.sunked = sunked;
        this.gameOver = gameOver;
        this.enemyField = enemyField;
        this.enemy = enemy;
    }

    public MessageMoveResult(byte id, boolean isGood, String message, long sessionId, int x, int y, boolean hitted,
            boolean sunked, boolean gameOver, int[][] enemyField) {
        super(id, isGood, message);
        this.sessionId = sessionId;
        this.x = x;
        this.y = y;
        this.hitted = hitted;
        this.sunked = sunked;
        this.gameOver = gameOver;
        this.enemyField = enemyField;
    }

    public long getSessionId() {
        return sessionId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean getHitted() {
        return hitted;
    }

    public boolean getSunked() {
        return sunked;
    }

    public boolean getGameOver() {
        return gameOver;
    }

    public int[][] getEnemyField() {
        return enemyField;
    }

    public boolean getEnemy()
    {
        return enemy;
    }
}
