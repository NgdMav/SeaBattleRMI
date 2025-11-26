package seaBattle.protocol.messages.messagesResponse;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResponse;

public class MessageChallengeFinal extends MessageResponse {

    private static final long serialVersionUID = 1L;

    public MessageChallengeFinal(int resultCode, String message, long sessionId) {
        super(Protocol.CMD_CHALLENGE_FINAL, resultCode, message);
        setSessionId(sessionId);
    }

    public MessageChallengeFinal(long sessionId) {
        super(Protocol.CMD_CHALLENGE_FINAL);
        setSessionId(sessionId);
    }
}
