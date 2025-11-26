package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageMove extends MessageRequest {

    private static final long serialVersionUID = 1L;

    private int x;
    private int y;

    public MessageMove(String from, long sessionId, int x, int y) {
        super(Protocol.CMD_MOVE, from, sessionId);
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
