package seaBattle.protocol.messages.messages;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.Message;

public class MessageIgnore extends Message {

    private static final long serialVersionUID = 1L;

    public MessageIgnore()
    {
        super(Protocol.CMD_IGNORE);
    }
}
