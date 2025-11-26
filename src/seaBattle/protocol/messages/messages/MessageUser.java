package seaBattle.protocol.messages.messages;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.Message;

public class MessageUser extends Message {

	private static final long serialVersionUID = 1L;

	public MessageUser() {
		super(Protocol.CMD_USER);
	}
}