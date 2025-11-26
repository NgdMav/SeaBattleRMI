package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessagePlaceShipsResult extends MessageResult {

	private static final long serialVersionUID = 1L;

	private boolean good = false;

	public MessagePlaceShipsResult() {
		super(Protocol.CMD_PLACE_SHIPS_RESULT);
	}

	public MessagePlaceShipsResult(boolean isGood, String message) {
		super(Protocol.CMD_PLACE_SHIPS_RESULT, isGood, message);
		this.good = isGood;
	}

	public boolean getGood()
	{
		return good;
	}
}
