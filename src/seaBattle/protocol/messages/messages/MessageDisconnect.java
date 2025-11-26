package seaBattle.protocol.messages.messages;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.Message;

public class MessageDisconnect extends Message {

	private static final long serialVersionUID = 1L;

	public MessageDisconnect() {
		super(Protocol.CMD_DISCONNECT);
	}

}
