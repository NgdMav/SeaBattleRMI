package seaBattle.server;

import seaBattle.client.ClientCallback;
import seaBattle.gameLogic.GameSession;
import seaBattle.protocol.messages.messages.*;
import seaBattle.protocol.messages.messagesRequest.*;
import seaBattle.protocol.messages.messagesResponse.MessageChallengeResponse;
import seaBattle.protocol.messages.messagesResponse.MessageGetFieldResult;
import seaBattle.protocol.messages.messagesResult.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SeaBattleServiceImpl extends UnicastRemoteObject  implements SeaBattleService{

    protected SeaBattleServiceImpl() throws RemoteException {}

    private final ConcurrentHashMap<String, ClientCallback> callbacks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, GameSession> sessions = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Challenge> challenges = new ConcurrentHashMap<>();

    private long sessionId = 1000000;
    private long challengeId = 1000000;

    private synchronized long getNextSessionId(){return  sessionId++;}
    private synchronized long getNextChallengeId(){return  challengeId++;}

    private void log(String tag, String text) {
        System.out.printf("[%tT] [%s] %s%n", System.currentTimeMillis(), tag, text);
    }

    @Override
    public MessagePong ping(MessagePing ping) throws RemoteException {
        return new MessagePong();
    }

    @Override
    public MessageConnectResult connect(MessageConnect req) throws RemoteException {
        String nic = req.getNic();

        if (nic == null) {
            return new MessageConnectResult(false, "NIC is null");
        }

        if (users.containsKey(nic)) {
            return new MessageConnectResult(false, "NIC is already in use");
        }

        users.put(nic, req.getFullName());
        callbacks.put(nic, req.getCallback());

        log("CONNECT", "User joined: " + nic + " as " + req.getFullName());

        return new MessageConnectResult();
    }

    @Override
    public MessageError disconnect(MessageDisconnect req) throws RemoteException {
        String nic = req.getFrom();

        users.remove(nic);
        callbacks.remove(nic);

        List<Long> toRemove = new ArrayList<>();
        for (Map.Entry<Long, GameSession> e : sessions.entrySet()) {
            GameSession gs = e.getValue();
            if (gs.getPlayerA().getNic().equals(nic) || gs.getPlayerB().getNic().equals(nic)) {
                toRemove.add(e.getKey());

                String opp = gs.getEnemyNic(nic);
                ClientCallback cb = callbacks.get(opp);

                if (cb != null) {
                    cb.onGameOver(new MessageGameOver(true, "Opponent disconnected", gs.getSessionId(), opp));
                }
            }
        }

        for (Long id : toRemove) {
            sessions.remove(id);
        }

        toRemove = new ArrayList<>();
        for (Map.Entry<Long, Challenge> e : challenges.entrySet()) {
            Challenge ch = e.getValue();
            if (ch.getFromNic().equals(nic) || ch.getToNic().equals(nic)) {
                toRemove.add(e.getKey());
            }
        }

        for (Long id : toRemove) {
            challenges.remove(id);
        }

        log("DISCONNECT", "User left: " + nic);

        return new MessageError("Disconnected");
    }

    @Override
    public MessageUserResult userList(MessageUser req) throws RemoteException {
        return new MessageUserResult(users.keySet().toArray(new String[0]));
    }

    @Override
    public MessageChallengeSuccessfullySend createChallenge(MessageChallenge req) throws RemoteException {

        String from = req.getFromNic();
        String to = req.getToNic();

        ClientCallback target = callbacks.get(to);
        if (target == null) {
            return new MessageChallengeSuccessfullySend(false, "server", -1);
        }

        long challengeID = getNextChallengeId();

        Challenge challenge = new Challenge(challengeID, from, to);
        challenges.put(challengeID, challenge);

        log("CHALLENGE","Created: " + challengeID + " " + from + " -> " + to);

        target.onChallengeRequest(new MessageChallengeRequest(from, challengeID));
        return new MessageChallengeSuccessfullySend(true, from, challengeID);
    }

    @Override
    public MessageChallengeResult answerChallenge(MessageChallengeResponse resp) throws RemoteException {
        return null;
    }

    @Override
    public MessagePlaceShipsResult placeShips(MessagePlaceShips req) throws RemoteException {
        return null;
    }

    @Override
    public MessageOpponentReady readyToPlay(MessageReadyToPlay req) throws RemoteException {
        return null;
    }

    @Override
    public MessageMoveResult move(MessageMove req) throws RemoteException {
        return null;
    }

    @Override
    public MessageGetFieldResult getField(MessageGetField req) throws RemoteException {
        return null;
    }

    @Override
    public MessageGameOver forfeit(MessageForfeit req) throws RemoteException {
        return null;
    }
}
