package seaBattle.protocol.messages.messages;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.Message;

public class MessagePing extends Message {

    private static final long serialVersionUID = 1L;

    public MessagePing() {
        super(Protocol.CMD_PING);
    }
}
