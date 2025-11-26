package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessageGameOver extends MessageResult {

    private static final long serialVersionUID = 1L;

    private long sessionId;
    private String winnerNic;

    public MessageGameOver(boolean isGood, String message, long sessionId, String winnerNic) {
        super(Protocol.CMD_GAMEOVER, isGood, message);
        this.sessionId = sessionId;
        this.winnerNic = winnerNic;
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getWinnerNic() {
        return winnerNic;
    }
}
