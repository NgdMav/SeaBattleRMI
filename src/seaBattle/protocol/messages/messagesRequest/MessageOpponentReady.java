package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageOpponentReady extends MessageRequest {

    private static final long serialVersionUID = 1L;

    public MessageOpponentReady(String from, long sessionId) {
        super(Protocol.CMD_OPPONENT_READY, from, sessionId);
    }
}