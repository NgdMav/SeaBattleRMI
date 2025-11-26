package seaBattle.protocol.messages.messagesResponse;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResponse;

public class MessageGetFieldResult extends MessageResponse {

    private static final long serialVersionUID = 1L;

    private int[][] field;

    public MessageGetFieldResult(int[][] field) {
        super(Protocol.CMD_GET_FIELD, Protocol.RESULT_CODE_OK, "OK");
        this.field = field;
    }

    public int[][] getField() {
        return field;
    }
}
