package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageChallengeRequest extends MessageRequest {

    private static final long serialVersionUID = 1L;

    private long challengeId;

    public MessageChallengeRequest(String from, long challengeId) {
        super(Protocol.CMD_CHALLENGE_REQUEST, from);
        this.challengeId = challengeId;
    }

    public long getChallengeId() {
        return challengeId;
    }
}
