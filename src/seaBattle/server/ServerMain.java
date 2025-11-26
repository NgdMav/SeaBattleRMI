package seaBattle.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import seaBattle.gameLogic.GameSession;
import seaBattle.gameLogic.Player.MoveResult;
import seaBattle.protocol.Protocol;
import seaBattle.protocol.cmd.CommandThread;
import seaBattle.protocol.messages.Message;
import seaBattle.protocol.messages.messages.MessageChallenge;
import seaBattle.protocol.messages.messages.MessageConnect;
import seaBattle.protocol.messages.messages.MessageUser;
import seaBattle.protocol.messages.messagesRequest.MessageChallengeRequest;
import seaBattle.protocol.messages.messagesRequest.MessageChallengeSuccesfullySend;
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
import seaBattle.protocol.messages.messagesResult.MessageConnectResult;
import seaBattle.protocol.messages.messagesResult.MessageError;
import seaBattle.protocol.messages.messagesResult.MessageGameOver;
import seaBattle.protocol.messages.messagesResult.MessageMoveResult;
import seaBattle.protocol.messages.messagesResult.MessagePlaceShipsResult;
import seaBattle.protocol.messages.messagesResult.MessagePong;
import seaBattle.protocol.messages.messagesResult.MessageUserResult;

public class ServerMain {

	private static int MAX_USERS = 100;

	public static void main(String[] args) {
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Server IP: " + ip);
			System.out.println("Server started on port: " + Protocol.PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try (ServerSocket serv = new ServerSocket(Protocol.PORT)) {
			ServerMain.log("SERVER", "Initialized");
			ServerStopThread tester = new ServerStopThread();
			tester.start();
			while (true) {
				Socket sock = accept(serv);
				if (sock != null) {
					if (ServerMain.getNumUsers() < ServerMain.MAX_USERS) {
						ServerMain.log("CONNECT", sock.getInetAddress().getHostName() + " connected");
						ServerClientHandler server = new ServerClientHandler(sock);
						server.start();
					} else {
						ServerMain.log("CONNECT", sock.getInetAddress().getHostName() + " connection rejected");
						sock.close();
					}
				}
				if (ServerMain.getStopFlag()) {
					break;
				}
			}
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			stopAllUsers();
			ServerMain.log("SERVER", "stopped");
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}

	public static Socket accept(ServerSocket serv) {
		assert (serv != null);
		try {
			serv.setSoTimeout(1000);
			Socket sock = serv.accept();
			return sock;
		} catch (SocketException e) {
		} catch (IOException e) {
		}
		return null;
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

class ServerStopThread extends CommandThread {

	static final String CMD_QUIT = "q";
	static final String CMD_QUIT_LONG = "quit";

	private final Scanner fin;

	public ServerStopThread() {
		fin = new Scanner(System.in);
		ServerMain.setStopFlag(false);
		putHandler(CMD_QUIT, CMD_QUIT_LONG, errorCode -> onCmdQuit());
		this.setDaemon(true);
		log("Admin console ready. Commands: users | sessions | challenges | info | quit");
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}

			if (!fin.hasNextLine())
				continue;
			String cmd = fin.nextLine().trim().toLowerCase();
			if (cmd.isEmpty())
				continue;

			switch (cmd) {
				case "q":
				case "quit":
					if (onCmdQuit())
						return;
					break;
				case "users":
					ServerMain.printUsers();
					break;
				case "sessions":
					ServerMain.printSessions();
					break;
				case "challenges":
					ServerMain.printChallenges();
					break;
				case "info":
					ServerMain.printServerInfo();
					break;
				default:
					log("Unknown command: " + cmd);
			}
		}
	}

	public boolean onCmdQuit() {
		log("Stopping server...");
		fin.close();
		ServerMain.setStopFlag(true);
		return true;
	}

	private void log(String msg) {
		System.out.printf("[%tT] [ADMIN] %s%n", System.currentTimeMillis(), msg);
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

						case Protocol.CMD_PING:
							os.writeObject(new MessagePong());
							break;

						case Protocol.CMD_CONNECT:
							if (!connect((MessageConnect) msg))
								return;
							break;

						case Protocol.CMD_DISCONNECT:
							synchronized (ServerMain.syncChallenges) {
								List<Long> challengesToRemove = new ArrayList<>();
								for (Challenge challenge : ServerMain.challenges.values()) {
									if (challenge.getFromNic().equals(userNic)
											|| challenge.getToNic().equals(userNic)) {
										challengesToRemove.add(challenge.getId());

										// Уведомляем другую сторону о отмене вызова
										String otherUser = challenge.getFromNic().equals(userNic) ? challenge.getToNic()
												: challenge.getFromNic();
										ServerClientHandler otherHandler = ServerMain.getUser(otherUser);
										if (otherHandler != null) {
											otherHandler.sendMessage(
													new MessageError("Challenge cancelled - user disconnected"));
										}
									}
								}
								for (Long challengeId : challengesToRemove) {
									ServerMain.removeChallenge(challengeId);
								}
							}
							synchronized (ServerMain.syncSession) {
								List<Long> sessionsToRemove = new ArrayList<>();
								for (GameSession gameSession : ServerMain.gameSessions.values()) {
									if (gameSession.getPlayerA().getNic().equals(userNic) ||
											gameSession.getPlayerB().getNic().equals(userNic)) {

										sessionsToRemove.add(gameSession.getSessionId());

										String opponentNic = gameSession.getEnemyNic(userNic);
										ServerClientHandler opponentHandler = ServerMain.getUser(opponentNic);
										if (opponentHandler != null) {
											opponentHandler.sendMessage(new MessageGameOver(
													true,
													"Opponent disconnected - you win!",
													gameSession.getSessionId(),
													opponentNic));

											// Также отправляем сообщение об ошибке для информации
											opponentHandler.sendMessage(new MessageError(
													"Your opponent has disconnected from the game"));
										}

										// Завершаем игровую сессию
										gameSession.gameEnd();
									}
								}
								for (Long sessionId : sessionsToRemove) {
									ServerMain.setSession(sessionId, null);
								}
							}
							//unregister();
							
							ServerMain.log("DISCONNECT", "User " + userNic + " fully disconnected");
							return;

						case Protocol.CMD_USER:
							user((MessageUser) msg);
							break;

						case Protocol.CMD_CHALLENGE:
							MessageChallenge challenge = (MessageChallenge) msg;
							ServerClientHandler target = ServerMain.getUser(challenge.getToNic());
							if (target != null) {
								long cid = ServerMain.nextChallengeId();
								Challenge ch = new Challenge(cid, userNic, challenge.getToNic());
								ServerMain.registerChallenge(ch);
								String from = challenge.getFromNic();
								os.writeObject(new MessageChallengeSuccesfullySend(from, cid));
								target.sendMessage(new MessageChallengeRequest(userNic, cid));
								ServerMain.log("CHALLENGE",
										"Created: " + cid + " " + userNic + " - " + challenge.getToNic());
							} else {
								os.writeObject(new MessageError("Target player not found"));
							}
							break;

						case Protocol.CMD_CHALLENGE_RESPONSE:
							MessageChallengeResponse resp = (MessageChallengeResponse) msg;
							Challenge ch = ServerMain.getChallenge(resp.getChallengeId());

							if (ch == null) {
								os.writeObject(new MessageError("Challenge not found"));
								break;
							}

							ServerClientHandler initiator = ServerMain.getUser(ch.getFromNic());
							if (initiator == null) {
								os.writeObject(new MessageError("Initiator not found"));
								ServerMain.removeChallenge(ch.getId());
								break;
							}

							if (resp.getAccepted()) {
								long sessionId = ServerMain.nextId();
								GameSession session = new GameSession(sessionId, ch.getFromNic(), ch.getToNic(), null,
										null);
								ServerMain.registerSession(sessionId, session);

								initiator.sendMessage(new MessageGameStart("Server", sessionId, userNic, true));
								sendMessage(new MessageGameStart("Server", sessionId, initiator.userNic, false));

								ServerMain.log("GAME SESSION", "Started: " + sessionId);
							} else {
								sendMessage(new MessageChallengeResult(false,
										"You declined challenge" , ch.getId()));
								initiator.sendMessage(new MessageChallengeResult(false,
										"Challenge declined by " + ch.getToNic(), ch.getId()));
							}

							ServerMain.removeChallenge(ch.getId());
							break;

						case Protocol.CMD_SHIP_PLACE:
							MessagePlaceShips mPlaceShips = (MessagePlaceShips) msg;
							GameSession session = ServerMain.getSession(mPlaceShips.getSessionId());
							boolean good = session.setPlaceShip(mPlaceShips.getFrom(), mPlaceShips.getShips());
							sendMessage(new MessagePlaceShipsResult(good, ""));
							break;
							
						case Protocol.CMD_READY:
							MessageReadyToPlay mready = (MessageReadyToPlay) msg; 
							session = ServerMain.getSession(mready.getSessionId());
							String currentPlayer = mready.getFrom();
							String enemyNic = session.getEnemyNic(currentPlayer);
							ServerClientHandler enemyP = ServerMain.getUser(enemyNic);
							if (enemyP != null) {
								enemyP.sendMessage(new MessageOpponentReady(currentPlayer, session.getSessionId()));
							}
							boolean start = session.playerReady(mready.getFrom());
							if (start) {
								ServerClientHandler enemy = ServerMain.getUser(session.getEnemyNic(mready.getFrom()));
								sendMessage(new MessageReadyToPlay(session.getToStart(), session.getSessionId()));
								enemy.sendMessage(new MessageReadyToPlay(session.getToStart(), session.getSessionId()));
							}
							break;

						case Protocol.CMD_MOVE:
							MessageMove msgmove = (MessageMove) msg;
							session = ServerMain.getSession(msgmove.getSessionId());
							try {
								MoveResult res = session.move(msgmove.getFrom(), msgmove.getX(), msgmove.getY());

								if (!res.gameOver) {
									sendMessage(new MessageMoveResult(true,
											msgmove.getFrom() + " move done",
											msgmove.getSessionId(), msgmove.getX(),
											msgmove.getY(), res.hitted, res.sunked,
											res.gameOver, res.field, true));

									ServerClientHandler enemy = ServerMain
											.getUser(session.getEnemyNic(msgmove.getFrom()));
									enemy.sendMessage((new MessageMoveResult(true,
											msgmove.getFrom() + " move done",
											msgmove.getSessionId(), msgmove.getX(),
											msgmove.getY(), res.hitted, res.sunked,
											res.gameOver, res.field, false)));
								}

								if (res.gameOver) {
									sendMessage(new MessageGameOver(true, "Game over", msgmove.getSessionId(),
											msgmove.getFrom()));
									ServerClientHandler enemy = ServerMain
											.getUser(session.getEnemyNic(msgmove.getFrom()));
									enemy.sendMessage(new MessageGameOver(true, "Game over", msgmove.getSessionId(),
											msgmove.getFrom()));
								}
							} catch (IllegalStateException e) {
								sendMessage(new MessageError("Not your turn, wait for opponent!"));
							}
							break;

						case Protocol.CMD_FORFEIT:
							MessageForfeit msgforfeit = (MessageForfeit) msg;
							session = ServerMain.getSession(msgforfeit.getSessionId());
							session.gameEnd();

							ServerClientHandler enemy = ServerMain.getUser(session.getEnemyNic(msgforfeit.getFrom()));
							sendMessage(
									new MessageGameOver(true, "Game over", msgforfeit.getSessionId(), enemy.userNic));
							enemy.sendMessage(
									new MessageGameOver(true, "Game over", msgforfeit.getSessionId(), enemy.userNic));

							break;

						case Protocol.CMD_GET_FIELD:
							MessageGetField msggetf = (MessageGetField) msg;
							session = ServerMain.getSession(msggetf.getSessionId());
							int[][] field = session.getField(msggetf.getFrom());
							sendMessage(new MessageGetFieldResult(field));
							break;
						
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

	boolean connect(MessageConnect msg) throws IOException {

		ServerClientHandler old = register(msg.getNic(), msg.getFullName());
		if (old == null) {
			os.writeObject(new MessageConnectResult());
			return true;
		} else {
			os.writeObject(
					new MessageConnectResult(false, "User " + old.userFullName + " already connected as " + userNic));
			return false;
		}
	}

	void user(MessageUser msg) throws IOException {

		String[] nics = ServerMain.getUsers();
		if (nics != null)
			os.writeObject(new MessageUserResult(nics));
		else
			os.writeObject(new MessageUserResult(false, "Unable to get users list", null));
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