package seaBattle.protocol.cmd;

public interface CmdHandler {
	boolean onCommand(int[] errorCode);
}