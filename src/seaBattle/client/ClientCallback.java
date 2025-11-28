package seaBattle.client;

import seaBattle.protocol.messages.messagesRequest.MessageChallengeRequest;
import seaBattle.protocol.messages.messagesRequest.MessageGameStart;
import seaBattle.protocol.messages.messagesRequest.MessageOpponentReady;
import seaBattle.protocol.messages.messagesRequest.MessageReadyToPlay;
import seaBattle.protocol.messages.messagesResult.MessageError;
import seaBattle.protocol.messages.messagesResult.MessageGameOver;
import seaBattle.protocol.messages.messagesResult.MessageMoveResult;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void onChallengeRequest(MessageChallengeRequest request) throws RemoteException;
    void onGameStart(MessageGameStart messageGameStart) throws RemoteException;
    void onMoveResult(MessageMoveResult messageMoveResult) throws RemoteException;
    void onGameOver(MessageGameOver messageGameOver) throws RemoteException;
    void onError(MessageError messageError) throws RemoteException;
    void onReadyToPlay(MessageReadyToPlay messageReadyToPlay) throws RemoteException;
    void onOpponentReadyToPlay(MessageOpponentReady messageReadyToPlay) throws RemoteException;
}
