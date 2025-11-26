package seaBattle.protocol.messages.messagesRequest;

import java.util.List;

import seaBattle.gameLogic.Ship;
import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.MessageRequest;

public class MessagePlaceShips extends MessageRequest {

    private static final long serialVersionUID = 1L;

    private List<Ship> ships;

    public MessagePlaceShips(String from, long sessionId, List<Ship> ships) {
        super(Protocol.CMD_SHIP_PLACE, from, sessionId);
        this.ships = ships;
    }

    public List<Ship> getShips() {
        return ships;
    }
}
