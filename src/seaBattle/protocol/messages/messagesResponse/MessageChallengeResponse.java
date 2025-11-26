package seaBattle.protocol.messages.messagesResponse;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResponse;

public class MessageChallengeResponse extends MessageResponse {

    private static final long serialVersionUID = 1L;

    private long challengeId;
    private boolean accepted;

    public MessageChallengeResponse(int resultCode, String message, long challengeId, boolean accepted) {
        super(Protocol.CMD_CHALLENGE_RESPONSE, resultCode, message);
        this.challengeId = challengeId;
        this.accepted = accepted;
    }

    public MessageChallengeResponse(long challengeId, boolean accepted) {
        super(Protocol.CMD_CHALLENGE_RESPONSE);
        this.challengeId = challengeId;
        this.accepted = accepted;
    }

    public long getChallengeId() {
        return challengeId;
    }

    public boolean getAccepted() {
        return accepted;
    }
}
