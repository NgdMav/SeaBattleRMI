package seaBattle.protocol.messages.messagesRequest;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessageGameStart extends MessageRequest {

    private static final long serialVersionUID = 1L;

    private String opponentNic;
    private boolean yourTurn;

    public MessageGameStart(String fromNic, long sessionId, String opponentNic, boolean yourTurn) {
        super(Protocol.CMD_GAME_STARTS, fromNic, sessionId);
        this.opponentNic = opponentNic;
        this.yourTurn = yourTurn;
    }

    public String getOppNic() {
        return opponentNic;
    }

    public boolean isYourTurn() {
        return yourTurn;
    }
}
