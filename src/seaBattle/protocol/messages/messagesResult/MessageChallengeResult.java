package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessageChallengeResult extends MessageResult {

	private static final long serialVersionUID = 1L;

	private long challengeId;

	public MessageChallengeResult(long challengeId) {
		super(Protocol.CMD_CHALLENGE);
		this.challengeId = challengeId;
	}

	public MessageChallengeResult(boolean isGood, String message, long challengeId) {
		super(Protocol.CMD_CHALLENGE, isGood, message);
		this.challengeId = challengeId;
	}

	public long getChallengeId() {
		return challengeId;
	}
}
