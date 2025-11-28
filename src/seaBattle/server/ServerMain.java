package seaBattle.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.ConcurrentHashMap;

import seaBattle.gameLogic.GameSession;
import seaBattle.gameLogic.Player.MoveResult;
import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.Message;
import seaBattle.protocol.messages.messages.MessageChallenge;
import seaBattle.protocol.messages.messages.MessageUser;
import seaBattle.protocol.messages.messagesRequest.MessageChallengeRequest;
import seaBattle.protocol.messages.messagesRequest.MessageChallengeSuccessfullySend;
import seaBattle.protocol.messages.messagesRequest.MessageForfeit;
import seaBattle.protocol.messages.messagesRequest.MessageGameStart;
import seaBattle.protocol.messages.messagesRequest.MessageGetField;
import seaBattle.protocol.messages.messagesRequest.MessageMove;
import seaBattle.protocol.messages.messagesRequest.MessageOpponentReady;
import seaBattle.protocol.messages.messagesRequest.MessagePlaceShips;
import seaBattle.protocol.messages.messagesRequest.MessageReadyToPlay;
import seaBattle.protocol.messages.messagesResponse.MessageChallengeResponse;
import seaBattle.protocol.messages.messagesResponse.MessageGetFieldResult;
import seaBattle.protocol.messages.messagesResult.MessageChallengeResult;
import seaBattle.protocol.messages.messagesResult.MessageError;
import seaBattle.protocol.messages.messagesResult.MessageGameOver;
import seaBattle.protocol.messages.messagesResult.MessageMoveResult;
import seaBattle.protocol.messages.messagesResult.MessagePlaceShipsResult;
import seaBattle.protocol.messages.messagesResult.MessageUserResult;

public class ServerMain {

	private static int MAX_USERS = 100;

	public static void main(String[] args) throws RemoteException {
        try {
            SeaBattleServiceImpl serviceImpl = new SeaBattleServiceImpl();
            String name = System.getProperty("servername", "FirstRemote");
            LocateRegistry.createRegistry(Protocol.PORT);
            Naming.rebind("SeaBattleService", serviceImpl);
            ServerMain.log("SERVER",name + " is open and ready for customers.");
        }
        catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java [-Dservername=<name>] " +
		            "RemoteBankServer");
            System.exit(1); // Force exit because there may be RMI threads
        }
	}

	private static void stopAllUsers() {
		String[] nic = getUsers();
		for (String user : nic) {
			ServerClientHandler ut = getUser(user);
			if (ut != null) {
				ut.disconnect();
			}
		}
	}

	private static Object syncFlags = new Object();
	private static boolean stopFlag = false;

	public static boolean getStopFlag() {
		synchronized (ServerMain.syncFlags) {
			return stopFlag;
		}
	}

	public static void setStopFlag(boolean value) {
		synchronized (ServerMain.syncFlags) {
			stopFlag = value;
		}
	}

	private static Object syncUsers = new Object();
	private static ConcurrentHashMap<String, ServerClientHandler> users = new ConcurrentHashMap<String, ServerClientHandler>();

	public static ServerClientHandler getUser(String userNic) {
		synchronized (ServerMain.syncUsers) {
			return ServerMain.users.get(userNic);
		}
	}

	public static ServerClientHandler registerUser(String userNic, ServerClientHandler user) {
		synchronized (ServerMain.syncUsers) {
			ServerClientHandler old = ServerMain.users.get(userNic);
			if (old == null) {
				ServerMain.users.put(userNic, user);
			}
			return old;
		}
	}

	public static ServerClientHandler setUser(String userNic, ServerClientHandler user) {
		synchronized (ServerMain.syncUsers) {
			if (user == null) {
				return ServerMain.users.remove(userNic); // Используйте remove вместо put
			} else {
				return ServerMain.users.put(userNic, user);
			}
		}
	}

	public static String[] getUsers() {
		synchronized (ServerMain.syncUsers) {
			return ServerMain.users.keySet().toArray(new String[0]);
		}
	}

	public static int getNumUsers() {
		synchronized (ServerMain.syncUsers) {
			return ServerMain.users.keySet().size();
		}
	}

	public static Object syncSession = new Object();
	public static ConcurrentHashMap<Long, GameSession> gameSessions = new ConcurrentHashMap<Long, GameSession>();
	private static long id = 1000000;

	public static long nextId() {
		return ++id;
	}

	public static GameSession getSession(long sessionId) {
		synchronized (ServerMain.syncSession) {
			return ServerMain.gameSessions.get(sessionId);
		}
	}

	public static GameSession registerSession(long sessionId, GameSession session) {
		synchronized (ServerMain.syncSession) {
			GameSession old = ServerMain.gameSessions.get(sessionId);
			if (old == null) {
				ServerMain.gameSessions.put(sessionId, session);
			}
			return old;
		}
	}

	public static GameSession setSession(long sessionId, GameSession session) {
		synchronized (ServerMain.syncSession) {
			if (session == null) {
				return ServerMain.gameSessions.remove(sessionId);
			}
			else
			{
				return ServerMain.gameSessions.put(sessionId, session);
			}
		}
	}

	public static Object[] getSessions() {
		synchronized (ServerMain.syncSession) {
			return ServerMain.gameSessions.keySet().toArray();
		}
	}

	public static int getNumSessions() {
		synchronized (ServerMain.syncSession) {
			return ServerMain.gameSessions.keySet().size();
		}
	}

	public static final Object syncChallenges = new Object();
	public static final ConcurrentHashMap<Long, Challenge> challenges = new ConcurrentHashMap<>();

	public static Challenge getChallenge(long id) {
		synchronized (syncChallenges) {
			return challenges.get(id);
		}
	}

	public static void registerChallenge(Challenge challenge) {
		synchronized (syncChallenges) {
			challenges.put(challenge.getId(), challenge);
		}
	}

	public static void removeChallenge(long id) {
		synchronized (syncChallenges) {
			challenges.remove(id);
		}
	}

	private static long challengeId = 10000;

	public static long nextChallengeId() {
		return ++challengeId;
	}

	public static void log(String tag, String msg) {
		System.out.printf("[%tT] [%s] %s%n", System.currentTimeMillis(), tag, msg);
	}

	public static void printUsers() {
		String[] users = getUsers();
		log("ADMIN", "Connected users: " + users.length);
		for (String u : users) {
			log("USER", " - " + u);
		}
	}

	public static void printSessions() {
		Object[] sessions = getSessions();
		log("ADMIN", "Active game sessions: " + sessions.length);
		for (Object s : sessions) {
			GameSession session = getSession((Long) s);
			if (session != null)
				log("SESSION",
						"ID=" + s + " | " + session.getPlayerA().getNic() + " vs " + session.getPlayerB().getNic());
		}
	}

	public static void printChallenges() {
		synchronized (syncChallenges) {
			log("ADMIN", "Active challenges: " + challenges.size());
			for (Challenge ch : challenges.values()) {
				log("CHALLENGE", "ID=" + ch.getId() + " | " + ch.getFromNic() + " → " + ch.getToNic());
			}
		}
	}

	public static void printServerInfo() {
		log("INFO", String.format("Users: %d | Sessions: %d | Challenges: %d",
				getNumUsers(), getNumSessions(), challenges.size()));
	}
}

class ServerClientHandler extends Thread {

	private Socket sock;
	private ObjectOutputStream os;
	private ObjectInputStream is;
	private InetAddress addr;

	private String userNic = null;
	private String userFullName;

	public ServerClientHandler(Socket s) throws IOException {
		sock = s;
		// s.setSoTimeout(1000);
		os = new ObjectOutputStream(s.getOutputStream());
		is = new ObjectInputStream(s.getInputStream());
		addr = s.getInetAddress();
		this.setDaemon(true);
	}

	public void run() {
		try {
			while (true) {
				Message msg = null;
				try {
					msg = (Message) is.readObject();
				} catch (java.io.EOFException e) {
					break;
				} catch (SocketException e) {
					break;
				} catch (IOException e) {
					if (!sock.isClosed()) {
						ServerMain.log("USER", "IO error for " + userNic + ": " + e.getMessage());
					}
					break;
				} catch (ClassNotFoundException e) {
					ServerMain.log("USER", "Invalid message from " + userNic + ": " + e.getMessage());
					continue;
				}

				if (msg == null)
					continue;
				if (msg != null)
					switch (msg.getID()) {
						
						case Protocol.CMD_IGNORE:
							break;
					}
			}
		} catch (IOException e) {
			ServerMain.log("USER", "Unexpected error for " + userNic + ": " + e.getMessage());
		} finally {
			disconnect();
		}
	}

	public synchronized void sendMessage(Message msg) {
		try {
			os.writeObject(msg);
			os.flush();
			ServerMain.log("MESSAGE", "Send to " + userNic + " (type = " + msg.getClass().toString() + ")");
		} catch (IOException e) {
			ServerMain.log("MESSAGE", "Error sending to " + userNic + ": " + e.getMessage());
		}
	}

	private boolean disconnected = false;

	public void disconnect() {
		if (!disconnected)
			try {
				ServerMain.log("USER", addr.getHostName() + " disconnected (UserNic = " + userNic + ")");
				unregister();
				os.close();
				is.close();
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				this.interrupt();
				disconnected = true;
			}
	}

	private void unregister() {
		if (userNic != null) {
			ServerMain.setUser(userNic, null);
			userNic = null;
		}
	}

	private ServerClientHandler register(String nic, String name) {
		ServerClientHandler old = ServerMain.registerUser(nic, this);
		if (old == null) {
			if (userNic == null) {
				userNic = nic;
				userFullName = name;
				ServerMain.log("USER", "User \'" + name + "\' registered as \'" + nic + "\'");
			}
		}
		return old;
	}
}