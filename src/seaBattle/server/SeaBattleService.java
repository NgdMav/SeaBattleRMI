package seaBattle.server;

import seaBattle.protocol.messages.MessageResult;
import seaBattle.protocol.messages.messages.*;
import seaBattle.protocol.messages.messagesRequest.*;
import seaBattle.protocol.messages.messagesResponse.MessageChallengeResponse;
import seaBattle.protocol.messages.messagesResponse.MessageGetFieldResult;
import seaBattle.protocol.messages.messagesResult.*;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SeaBattleService extends Remote {
    MessagePong ping(MessagePing messagePing) throws RemoteException;

    MessageConnectResult connect(MessageConnect req) throws RemoteException;
    MessageResult disconnect(MessageDisconnect req) throws RemoteException;

    MessageUserResult userList(MessageUser req) throws RemoteException;

    MessageChallengeSuccessfullySend createChallenge(MessageChallenge req) throws RemoteException;
    MessageChallengeResult answerChallenge(MessageChallengeResponse resp) throws RemoteException;

    MessagePlaceShipsResult placeShips(MessagePlaceShips req) throws RemoteException;

    MessageReadyToPlay readyToPlay(MessageReadyToPlay req) throws RemoteException;

    MessageMoveResult move(MessageMove req) throws RemoteException;
    MessageGetFieldResult getField(MessageGetField req) throws RemoteException;
    MessageGameOver forfeit(MessageForfeit req) throws RemoteException;
}
