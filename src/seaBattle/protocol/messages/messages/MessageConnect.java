package seaBattle.protocol.messages.messages;

import seaBattle.client.ClientCallback;
import seaBattle.protocol.messages.Message;
import seaBattle.protocol.Protocol;

public class MessageConnect extends Message {

	private static final long serialVersionUID = 1L;

	private String userNic;
	private String userFullName;
    private ClientCallback callback;

	public MessageConnect(String userNic, String userFullName, ClientCallback callback) {
		super(Protocol.CMD_CONNECT);
		this.userNic = userNic;
		this.userFullName = userFullName;
        this.callback = callback;
	}

	public String getNic() {
		return userNic;
	}

	public String getFullName() {
		return userFullName;
	}

    public ClientCallback getCallback() {
        return callback;
    }
}