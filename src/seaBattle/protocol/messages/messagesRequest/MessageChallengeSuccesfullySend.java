package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageChallengeSuccesfullySend extends MessageRequest
{
    private static final long serialVersionUID = 1L;

    private long challengeId;

    public MessageChallengeSuccesfullySend(String from, long challengeId)
    {
        super(Protocol.CMD_CHALLENGE_SUCCESFULLY_SEND, from);
        this.challengeId = challengeId;
    }
}
