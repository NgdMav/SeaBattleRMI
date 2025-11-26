package seaBattle.protocol.messages;

public class MessageRequest extends Message {

    private static final long serialVersionUID = 1L;

    private String from;
    private long sessionId;

    protected MessageRequest(byte id, String from) {
        super(id);
        this.from = from;
    }

    protected MessageRequest(byte id, String from, long sessionId) {
        super(id);
        this.from = from;
        this.sessionId = sessionId;
    }

    public String getFrom() {
        return from;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }
}
