package seaBattle.protocol.messages.messagesResult;

import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageResult;

public class MessageError extends MessageResult {

  private static final long serialVersionUID = 1L;

  public MessageError(String message) {
    super(Protocol.CMD_ERROR, false, message);
  }
}
