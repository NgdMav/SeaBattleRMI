package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageReadyToPlay extends MessageRequest {

    private static final long serialVersionUID = 1L;

    public MessageReadyToPlay(String from, long sessionId) {
        super(Protocol.CMD_READY, from, sessionId);
    }
}
