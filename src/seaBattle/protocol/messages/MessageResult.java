package seaBattle.protocol.messages;

import seaBattle.protocol.Protocol;

public class MessageResult extends Message {

	private static final long serialVersionUID = 1L;

	private int errorCode;

	public int getErrorCode() {
		return errorCode;
	}

	public boolean Error() {
		return errorCode == Protocol.RESULT_CODE_ERROR;
	}

	private String message;

	public String getMessage() {
		return message;
	}

	protected MessageResult() {
		super();
	}

	protected MessageResult(byte id, boolean isGood, String message) {

		super(id);
		if (!isGood) {
			this.errorCode = Protocol.RESULT_CODE_ERROR;
		} else {
			this.errorCode = Protocol.RESULT_CODE_OK;
		}
		this.message = message;
	}

	protected MessageResult(byte id) {

		super(id);
		this.errorCode = Protocol.RESULT_CODE_OK;
		this.message = "...OK...";
	}
}