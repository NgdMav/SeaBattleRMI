package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessageUserResult extends MessageResult {

	private static final long serialVersionUID = 1L;

	private String[] userNics = null;

	public MessageUserResult(String[] userNics) {
		super(Protocol.CMD_USER);
		this.userNics = userNics;
	}

	public MessageUserResult(boolean isGood, String message, String[] userNics) {
		super(Protocol.CMD_USER, isGood, message);
		this.userNics = userNics;
	}

	public String[] getNics() {
		return userNics;
	}
}