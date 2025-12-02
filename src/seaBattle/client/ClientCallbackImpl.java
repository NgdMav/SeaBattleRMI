package seaBattle.client;

import seaBattle.protocol.messages.messagesRequest.MessageChallengeRequest;
import seaBattle.protocol.messages.messagesRequest.MessageGameStart;
import seaBattle.protocol.messages.messagesRequest.MessageOpponentReady;
import seaBattle.protocol.messages.messagesRequest.MessageReadyToPlay;
import seaBattle.protocol.messages.messagesResult.MessageError;
import seaBattle.protocol.messages.messagesResult.MessageGameOver;
import seaBattle.protocol.messages.messagesResult.MessageMoveResult;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback{

    private final ClientMain clientMain;

    protected ClientCallbackImpl(ClientMain clientMain) throws RemoteException {
        this.clientMain = clientMain;
    }

    @Override
    public void onChallengeRequest(MessageChallengeRequest request) throws RemoteException {
        clientMain.onChallengeRequest(request);
    }

    @Override
    public void onGameStart(MessageGameStart messageGameStart) throws RemoteException {
        clientMain.onGameStart(messageGameStart);
    }

    @Override
    public void onMoveResult(MessageMoveResult messageMoveResult) throws RemoteException {
        clientMain.onMoveResult(messageMoveResult);
    }

    @Override
    public void onGameOver(MessageGameOver messageGameOver) throws RemoteException {
        clientMain.onGameOver(messageGameOver);
    }

    @Override
    public void onError(MessageError messageError) throws RemoteException {
        clientMain.onError(messageError);
    }

    @Override
    public void onReadyToPlay(MessageReadyToPlay messageReadyToPlay) throws RemoteException {
        clientMain.onReadyToPlay(messageReadyToPlay);
    }

    @Override
    public void onOpponentReadyToPlay(MessageOpponentReady messageReadyToPlay) throws RemoteException {
        clientMain.onOpponentReadyToPlay(messageReadyToPlay);
    }
}
