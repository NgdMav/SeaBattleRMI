package seaBattle.client;

import seaBattle.protocol.messages.messagesRequest.MessageChallengeRequest;
import seaBattle.protocol.messages.messagesRequest.MessageGameStart;
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

    }

    @Override
    public void onMoveResult(MessageMoveResult messageMoveResult) throws RemoteException {

    }

    @Override
    public void onGameOver(MessageGameOver messageGameOver) throws RemoteException {

    }

    @Override
    public void onError(MessageError messageError) throws RemoteException {

    }

    @Override
    public void onReadyToPlay(MessageReadyToPlay messageReadyToPlay) throws RemoteException {

    }
}
