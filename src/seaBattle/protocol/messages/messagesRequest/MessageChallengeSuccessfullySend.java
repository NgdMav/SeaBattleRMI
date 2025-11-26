package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageChallengeSuccessfullySend extends MessageRequest
{
    private static final long serialVersionUID = 1L;

    private long challengeId;
    private boolean successful;

    public MessageChallengeSuccessfullySend(boolean successful, String from, long challengeId)
    {
        super(Protocol.CMD_CHALLENGE_SUCCESFULLY_SEND, from);
        this.successful = successful;
        this.challengeId = challengeId;
    }
}
