package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessageConnectResult extends MessageResult {

	private static final long serialVersionUID = 1L;

	public MessageConnectResult() {
		super(Protocol.CMD_CONNECT);
	}

	public MessageConnectResult(boolean isGood, String message) {
		super(Protocol.CMD_CONNECT, isGood, message);
	}

}