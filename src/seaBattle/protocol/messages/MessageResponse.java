package seaBattle.protocol.messages;

import seaBattle.protocol.Protocol;

public class MessageResponse extends Message {

    private static final long serialVersionUID = 1L;

    private int resultCode;
    private String message;
    private long sessionId;

    protected MessageResponse(byte id, int resultCode, String message) {
        super(id);
        this.resultCode = resultCode;
        this.message = message;
    }

    protected MessageResponse(byte id) {
        super(id);
        this.resultCode = Protocol.RESULT_CODE_OK;
        this.message = "...OK...";
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isOk() {
        return resultCode == Protocol.RESULT_CODE_OK;
    }

    public boolean isDeclined() {
        return resultCode == Protocol.RESULT_CODE_DECLINE;
    }

    public boolean isError() {
        return resultCode == Protocol.RESULT_CODE_ERROR;
    }
}
