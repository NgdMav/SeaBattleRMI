package seaBattle.protocol.messages.messages;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.Message;

public class MessageDisconnect extends Message {

	private static final long serialVersionUID = 1L;

    String from;
    public MessageDisconnect(String from) {
        super(Protocol.CMD_DISCONNECT);
        this.from = from;
    }

	public MessageDisconnect() {
		super(Protocol.CMD_DISCONNECT);
	}

    public String getFrom() {
        return from;
    }
}
