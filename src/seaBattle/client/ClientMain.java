	package seaBattle.client;

    import java.rmi.Naming;
    import java.rmi.RemoteException;
	import java.util.List;
	import java.util.Scanner;

	import seaBattle.gameLogic.Ship;
	import seaBattle.protocol.Protocol;
	import seaBattle.protocol.messages.messages.MessageChallenge;
	import seaBattle.protocol.messages.messages.MessageConnect;
	import seaBattle.protocol.messages.messages.MessageDisconnect;
	import seaBattle.protocol.messages.messages.MessagePing;
	import seaBattle.protocol.messages.messages.MessageUser;
    import seaBattle.protocol.messages.messagesRequest.*;
	import seaBattle.protocol.messages.messagesResponse.MessageChallengeResponse;
	import seaBattle.protocol.messages.messagesResult.MessageConnectResult;
	import seaBattle.protocol.messages.messagesResult.MessageError;
	import seaBattle.protocol.messages.messagesResult.MessageGameOver;
	import seaBattle.protocol.messages.messagesResult.MessageMoveResult;
	import seaBattle.protocol.messages.messagesResult.MessageUserResult;
    import seaBattle.server.SeaBattleService;

	public class ClientMain 
	{

        private SeaBattleService server;

        private final ClientSession session;

        public ClientMain(String nick, String fullName) {
            this.session = new ClientSession(nick, fullName);
        }

		public static void main(String[] args) {
			if (args.length < 2 || args.length > 3) {
                System.out.println("Use: nickname fullname [host]");
                return;
            }

            String nick = args[0];
            String fullName = args[1];
            String host = args.length == 3 ? args[2] : "localhost";

            try {
                ClientMain client = new ClientMain(nick, fullName);
                client.start(host);
            } catch (Exception e) {
                e.printStackTrace();
            }
		}

        public void start(String host) throws Exception {

            System.out.println("Connecting to RMI registry...");
            server = (SeaBattleService) Naming.lookup("rmi://" + host + ":" + Protocol.PORT + "/SeaBattleService");

            System.out.println("Registering callback...");
            ClientCallbackImpl callback = new ClientCallbackImpl(this);

            System.out.println("Sending connect request...");

            MessageConnect req = new MessageConnect(session.userNickName, session.userFullName, callback);

            MessageConnectResult res = server.connect(req);

            if (!res.Error()) {
                System.out.println("Connected as " + session.userNickName);
                session.connected = true;
            } else {
                System.out.println("Connection error: " + res.getMessage());
                return;
            }

            System.out.println("Type 'help' for commands");
            commandLoop();
        }

        private void commandLoop() throws Exception {
            Scanner in = new Scanner(System.in);

            while (true) {
                System.out.print("> ");
                String cmd = in.nextLine().trim().toLowerCase();

                switch (cmd) {

                    case "help":
                        printHelp();
                        break;

                    case "ping":
                        server.ping(new MessagePing());
                        break;

                    case "users":
                        MessageUserResult users = server.userList(new MessageUser());
                        System.out.println("Active users:");
                        for (String u : users.getNics()) {
                            if (!u.equals(session.userNickName))
                                System.out.println(" - " + u);
                        }
                        break;

                    case "challenge":
                        System.out.print("Enter opponent nickname: ");
                        String to = in.nextLine().trim();
                        MessageChallengeSuccessfullySend cmsg =
                                server.createChallenge(new MessageChallenge(session.userNickName, to));

                        if (cmsg != null)
                            System.out.println("Challenge sent");
                        else
                            System.out.println("Failed");
                        break;

                    case "answer":
                        answerChallenge(in);
                        break;

                    case "place":
                        placeShips(in);
                        break;

                    case "ready":
                        ready();
                        break;

                    case "move":
                        move(in);
                        break;

                    case "disconnect":
                    case "quit":
                    case "exit":
                        disconnect();
                        return;

                    default:
                        System.out.println("Unknown command");
                }
            }
        }

		private void answerChallenge(Scanner in) throws Exception {
            if (session.challenges.isEmpty()) {
                System.out.println("No incoming challenges");
                return;
            }

            System.out.println("Active challenges:");
            for (ClientSession.ChallengeFromPlayer cfp : session.challenges) {
                System.out.println(cfp.playerNickName() + ": " + cfp.challengeID());
            }

            System.out.print("Enter challenge ID: ");
            long id = Long.parseLong(in.nextLine().trim());

            System.out.print("Accept? (Y/N): ");
            String ans = in.nextLine().trim().toUpperCase();

            boolean accepted = ans.equals("Y");
            server.answerChallenge(new MessageChallengeResponse(id, accepted));

            session.challenges.removeIf(c -> c.challengeID() == id);
        }

        private void placeShips(Scanner in) throws Exception {
            if (session.currentGameSessionID == null) {
                System.out.println("You are not in game");
                return;
            }

            System.out.print("Random ships? (Y/N): ");
            if (in.nextLine().trim().equalsIgnoreCase("Y")) {

                List<Ship> ships = ClientSession.randomShips();
                System.out.println("Ships placed:");
                ClientSession.printField(ClientSession.makeField(ships));

                server.placeShips(new MessagePlaceShips(
                        session.userNickName,
                        session.currentGameSessionID,
                        ships
                ));

            } else {
                System.out.println("Manual placement is not implemented in RMI client yet.");
            }
        }

        private void ready() throws Exception {
            if (session.currentGameSessionID == null) {
                System.out.println("No active game");
                return;
            }

            server.readyToPlay(new MessageReadyToPlay(
                    session.userNickName,
                    session.currentGameSessionID
            ));
        }

        private void move(Scanner in) throws Exception {
            if (!session.yourTurn) {
                System.out.println("Not your turn!");
                return;
            }

            System.out.print("X: ");
            int x = Integer.parseInt(in.nextLine().trim());
            System.out.print("Y: ");
            int y = Integer.parseInt(in.nextLine().trim());

            server.move(new MessageMove(
                    session.userNickName,
                    session.currentGameSessionID,
                    x, y
            ));
        }

        private void disconnect() throws RemoteException {
            server.disconnect(new MessageDisconnect(session.userNickName));
            System.out.println("Disconnected");
        }

        private void printHelp() {
            System.out.println("""
                    Commands:
                    ping
                    users
                    challenge
                    answer
                    place
                    ready
                    move
                    disconnect | exit
                    """);
        }

        // -------------------- CALLBACK ENTRY POINTS --------------------

        public void onChallengeRequest(MessageChallengeRequest req) {
            session.challenges.add(new ClientSession.ChallengeFromPlayer(
                    req.getChallengeId(), req.getFrom()
            ));

            System.out.println("\n*** Incoming challenge from " + req.getFrom() +
                    " (id=" + req.getChallengeId() + ") ***");
            System.out.println("Type 'answer' to respond");
        }

        public void onGameStart(MessageGameStart msg) {
            session.userOpponentNick = msg.getOppNic();
            session.currentGameSessionID = msg.getSessionId();
            session.gameStarted = false;
            session.shipsReady = false;
            session.playerReady = false;

            System.out.println("\n*** Game started vs " + msg.getOppNic() + " ***");
            System.out.println("Type 'place' to place ships");
        }

        public void onMoveResult(MessageMoveResult msg) {
            ClientSession.printMove(msg, session);
        }

        public void onGameOver(MessageGameOver msg) {
            System.out.println("\n*** GAME OVER ***");
            session.resetGame();
        }

        public void onError(MessageError msg) {
            System.out.println("SERVER ERROR: " + msg.getMessage());
        }

        public void onReadyToPlay(MessageReadyToPlay msg) {
            String starter = msg.getFrom();
            if (starter.equals(session.userNickName)) {
                session.yourTurn = true;
                System.out.println("Game started! Your turn first.");
            } else {
                session.yourTurn = false;
                System.out.println("Game started! Opponent starts.");
            }
        }

        public void onOpponentReady(MessageOpponentReady msg) {
            System.out.println("Opponent ready: " + msg.getFrom());
        }
	}
