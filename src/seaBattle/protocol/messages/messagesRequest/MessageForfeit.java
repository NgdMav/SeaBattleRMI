package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageForfeit extends MessageRequest {

	private static final long serialVersionUID = 1L;

	public MessageForfeit(String from, long sessionId) {
		super(Protocol.CMD_FORFEIT, from, sessionId);
	}
}
