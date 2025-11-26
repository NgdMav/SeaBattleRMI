package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageGetField extends MessageRequest {

    private static final long serialVersionUID = 1L;

    public MessageGetField(String from, long sessionId) {
        super(Protocol.CMD_GET_FIELD, from, sessionId);
    }
}
