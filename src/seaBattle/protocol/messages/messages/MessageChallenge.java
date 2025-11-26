package seaBattle.protocol.messages.messages;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.Message;

public class MessageChallenge extends Message {

    private static final long serialVersionUID = 1L;

    private String fromUserNic;
    private String toUserNic;

    public MessageChallenge(String fromUserNic, String toUserNic) {
        super(Protocol.CMD_CHALLENGE);
        this.fromUserNic = fromUserNic;
        this.toUserNic = toUserNic;
    }

    public String getFromNic() {
        return fromUserNic;
    }

    public String getToNic() {
        return toUserNic;
    }
}
