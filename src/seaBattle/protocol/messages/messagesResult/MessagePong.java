package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessagePong extends MessageResult {

    private static final long serialVersionUID = 1L;

    public MessagePong() {
        super(Protocol.CMD_PONG);
    }
}
