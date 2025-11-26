package seaBattle.protocol;

interface CMD {
	static final byte CMD_PING = 1;
	static final byte CMD_PONG = 2;
	static final byte CMD_CONNECT = 3;
	static final byte CMD_DISCONNECT = 4;
	static final byte CMD_USER = 5;
	static final byte CMD_CHALLENGE = 6;
	static final byte CMD_CHALLENGE_REQUEST = 7;
	static final byte CMD_CHALLENGE_RESPONSE = 8;
	static final byte CMD_CHALLENGE_FINAL = 9;
	static final byte CMD_GAME_STARTS = 10;
	static final byte CMD_SHIP_PLACE = 11;
	static final byte CMD_MOVE = 12;
	static final byte CMD_OPPONENT_MOVE = 13;
	static final byte CMD_GAMEOVER = 14;
	static final byte CMD_FORFEIT = 15;
	static final byte CMD_ERROR = 16;
	static final byte CMD_READY = 17;
	static final byte CMD_GET_FIELD = 18;
	static final byte CMD_CHALLENGE_SUCCESFULLY_SEND = 19;
	static final byte CMD_PLACE_SHIPS_RESULT = 20;
	static final byte CMD_OPPONENT_READY = 21;
	static final byte CMD_IGNORE = 22;
}

interface RESULT {
	static final int RESULT_CODE_OK = 0;
	static final int RESULT_CODE_DECLINE = 1;
	static final int RESULT_CODE_ERROR = -1;
}

interface PORT {
	static final int PORT = 4242;
}

public class Protocol implements CMD, RESULT, PORT {
	private static final byte CMD_MIN = CMD_PING;
	private static final byte CMD_MAX = CMD_GET_FIELD;

	public static boolean validID(byte id) {
		return id >= CMD_MIN && id <= CMD_MAX;
	}
}
