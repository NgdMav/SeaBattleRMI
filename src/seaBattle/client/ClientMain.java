package seaBattle.client;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import seaBattle.gameLogic.Ship;
import seaBattle.protocol.Protocol;
import seaBattle.protocol.messages.messages.*;
import seaBattle.protocol.messages.messagesRequest.*;
import seaBattle.protocol.messages.messagesResponse.MessageChallengeResponse;
import seaBattle.protocol.messages.messagesResponse.MessageGetFieldResult;
import seaBattle.protocol.messages.messagesResult.*;
import seaBattle.server.SeaBattleService;

public class ClientMain {
    
    private SeaBattleService server;
    private final ClientSession session;
    private final Scanner scanner;
    private boolean running = true;

    public ClientMain(String nick, String fullName) {
        this.session = new ClientSession(nick, fullName);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java ClientMain <nickname> <fullname> [host]");
            System.out.println("Example: java ClientMain player1 \"John Doe\" localhost");
            return;
        }

        String nick = args[0];
        String fullName = args[1];
        String host = args.length == 3 ? args[2] : "localhost";

        try {
            ClientMain client = new ClientMain(nick, fullName);
            client.start(host);
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void start(String host) throws Exception {
        System.out.println("\nConnecting to server at " + host + ":" + Protocol.PORT + "...");
        
        try {
            String url = "rmi://" + host + ":" + Protocol.PORT + "/SeaBattleService";
            server = (SeaBattleService) Naming.lookup(url);
            System.out.println("Connected to RMI registry");
        } catch (Exception e) {
            System.err.println("Cannot connect to server: " + e.getMessage());
            return;
        }

        System.out.println("Registering client callback...");
        ClientCallbackImpl callback = new ClientCallbackImpl(this);

        System.out.println("Authenticating as " + session.userNickName);
        MessageConnect req = new MessageConnect(session.userNickName, session.userFullName, callback);
        MessageConnectResult res = server.connect(req);

        if (!res.Error()) {
            System.out.println("Authentication successful!");
            session.connected = true;
        } else {
            System.out.println("Authentication failed: " + res.getMessage());
            return;
        }

        System.out.println("\nType <help> for available commands");
        
        commandLoop();
    }

    private void commandLoop() {
        while (running && session.connected) {
            try {
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();
                String[] args = parts.length > 1 ? 
                    java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                
                if (!processCommand(command, args)) {
                    System.out.println("Unknown command. Type <help> for list of commands.");
                }
                
            } catch (Exception e) {
                System.err.println("Command error: " + e.getMessage());
                if (e instanceof RemoteException) {
                    System.err.println("Lost connection to server!");
                    session.connected = false;
                    break;
                }
            }
        }
        
        scanner.close();
        System.out.println("Client terminated.");
    }

    private boolean processCommand(String command, String[] args) throws RemoteException {
        return switch (command) {
            case "help" -> {
                printHelp();
                yield true;
            }
            case "ping" -> handlePing();
            case "users" -> handleUsers();
            case "challenge" -> handleChallenge(args);
            case "answer" -> handleAnswerChallenge(args);
            case "listch" -> handleListChallenges();
            case "place" -> handlePlaceShips(args);
            case "field" -> handleGetField();
            case "ready" -> handleReady();
            case "move" -> handleMove(args);
            case "forfeit" -> handleForfeit();
            case "quit", "q" -> handleDisconnect();
            default -> false;
        };
    }

    private boolean handlePing() throws RemoteException {
        MessagePong pong = server.ping(new MessagePing());
        if (pong != null) {
            System.out.println("pong");
            return true;
        }
        return false;
    }

    private boolean handleUsers() throws RemoteException {
        MessageUserResult users = server.userList(new MessageUser());
        if (users.Error()) {
            System.out.println("Error getting user list: " + users.getMessage());
            return false;
        }
        
        String[] userList = users.getNics();
        System.out.println("Active users:");        
        for (String user : userList) {
            if (user.equals(session.userNickName)) {
            } else {
                System.out.println(user + "; ");
            }
        }
        return true;
    }

    private boolean handleChallenge(String[] args) throws RemoteException {
        String opponent;
        
        if (args.length > 0) {
            opponent = args[0];
        } else {
            System.out.print("Enter opponent nickname: ");
            opponent = scanner.nextLine().trim();
        }
        
        if (opponent.isEmpty()) {
            System.out.println("Opponent nickname cannot be empty");
            return false;
        }
        
        if (opponent.equals(session.userNickName)) {
            System.out.println("You cannot challenge yourself");
            return false;
        }
        
        MessageChallengeSuccessfullySend result = 
            server.createChallenge(new MessageChallenge(session.userNickName, opponent));
        
        if (result != null) {
            System.out.println("Challenge succesfully sent to " + opponent + " (ID: " + result.getSessionId() + ")");
            return true;
        } else {
            System.out.println("Failed to send challenge to " + opponent);
            return false;
        }
    }

    private boolean handleAnswerChallenge(String[] args) throws RemoteException {
        if (session.challenges.isEmpty()) {
            System.out.println("No pending challenges");
            return false;
        }
        
        long challengeId;
        boolean accepted;
        
        if (args.length >= 2) {
            try {
                challengeId = Long.parseLong(args[0]);
                accepted = args[1].equalsIgnoreCase("yes") || args[1].equalsIgnoreCase("Y");
            } catch (NumberFormatException e) {
                System.out.println("Invalid challenge ID format");
                return false;
            }
        } else {
            System.out.println("Pending challenges: ");
            
            for (ClientSession.ChallengeFromPlayer cfp : session.challenges) {
                System.out.println("ID: " + cfp.challengeID() + " From: " + cfp.playerNickName());
            }
            
            System.out.print("\nEnter challenge ID: ");
            challengeId = Long.parseLong(scanner.nextLine().trim());
            
            System.out.print("Accept challenge? (Y/N): ");
            String response = scanner.nextLine().trim().toLowerCase();
            accepted = response.equals("yes") || response.equals("y");
        }
        
        MessageChallengeResult result = server.answerChallenge(
            new MessageChallengeResponse(challengeId, accepted));
        
        if (result != null && !result.Error()) {
            if (accepted) {
                System.out.println("Challenge accepted!");
            } else {
                System.out.println("Challenge declined");
            }
            
            session.challenges.removeIf(c -> c.challengeID() == challengeId);
            return true;
        } else {
            System.out.println("Failed to answer challenge: " + 
                (result != null ? result.getMessage() : "Unknown error"));
            return false;
        }
    }

    private boolean handleListChallenges() {
        if (session.challenges.isEmpty()) {
            System.out.println("No pending challenges");
            return true;
        }
        
        System.out.println("Pending challenges:");
        
        for (ClientSession.ChallengeFromPlayer cfp : session.challenges) {
            System.out.print("Challenge ID: " + cfp.challengeID());
            System.out.println(" From: " + cfp.playerNickName());
        }
        
        System.out.println("\nUse: answer <id> <Y/N>  to respond");
        return true;
    }

    private boolean handlePlaceShips(String[] args) throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("You are not in a game");
            return false;
        }
        
        System.out.println("Do you wan't to randomise ships placement?(Y/N)");
        String answer = scanner.nextLine().trim().toLowerCase();
        boolean useRandom = answer.equals("yes") || answer.equals("y");

        List<Ship> ships;
        
        if (useRandom) {
            System.out.println("\nGenerating random ship placement...");
            ships = ClientSession.randomShips();
            System.out.println("Ships generated successfully");
            
            System.out.println("\nYour battlefield:");
            ClientSession.printField(ClientSession.makeField(ships));
        } else {
            ships = handleManualShipPlacement();
            if (ships == null) {
                return false;
            }
        }
        
        MessagePlaceShipsResult result = server.placeShips(
            new MessagePlaceShips(session.userNickName, session.currentGameSessionID, ships));
        
        if (!result.Error()) {
            System.out.println("Ships placed successfully!");
            System.out.println("Print <ready> to start the game");
            session.shipsReady = true;
            return true;
        } else {
            System.out.println("Failed to place ships: " + result.getMessage());
            return false;
        }
    }

    private List<Ship> handleManualShipPlacement() {
        System.out.println("Format: x y length orientation");
        System.out.println("  x, y: coordinates (1-10)");
        System.out.println("  length: ship length (1-4)");
        System.out.println("  orientation: h (horizontal) or v (vertical)");
        System.out.println("\nExample: 3 5 3 h  (3-length ship at (3,5) horizontal)");
        System.out.println("Type <done> when finished, <cancel> to abort");
        
        List<Ship> ships = new ArrayList<>();
        int[][] field = new int[12][12];
        int[] shipCounts = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        
        for (int i = 0; i < shipCounts.length; i++) {
            while (true) {
                System.out.print("Ship " + (i + 1) + "/10 (length " + shipCounts[i] + "): ");
                String input = scanner.nextLine().trim().toLowerCase();
                
                if (input.equals("cancel")) {
                    System.out.println("Placement cancelled");
                    return null;
                }
                
                if (input.equals("done")) {
                    System.out.println("You placed " + ships.size() + " ships. Need " + 
                        (shipCounts.length - ships.size()) + " more.");
                    continue;
                }
                
                String[] parts = input.split("\\s+");
                if (parts.length != 4) {
                    System.out.println("Invalid format. Use: x y length h/v");
                    continue;
                }
                
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int length = Integer.parseInt(parts[2]);
                    boolean vertical = parts[3].equals("v");
                    
                    if (length != shipCounts[i]) {
                        System.out.println("This ship must be length " + shipCounts[i]);
                        continue;
                    }
                    
                    Ship ship = new Ship(x, y, length, vertical);
                    
                    if (!ClientSession.canPlace(field, ship)) {
                        System.out.println("Cannot place ship there (overlap or out of bounds)");
                        continue;
                    }
                    
                    ships.add(ship);
                    ClientSession.placeOnField(field, ship);
                    
                    System.out.println("Ship placed at (" + x + "," + y + ") " + 
                        (vertical ? "vertical" : "horizontal"));
                    break;
                    
                } catch (Exception e) {
                    System.out.println("Invalid input: " + e.getMessage());
                }
            }
        }
        
        System.out.println("\nAll ships placed successfully!");
        System.out.println("\nFinal battlefield:");
        ClientSession.printField(field);
        
        return ships;
    }

    private boolean handleGetField() throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("You are not in a game");
            return false;
        }
        
        MessageGetFieldResult result = server.getField(
            new MessageGetField(session.userNickName, session.currentGameSessionID));
        
        if (result != null && result.getField() != null) {
            System.out.println("Your field: ");
            ClientSession.printField(result.getField());
            return true;
        } else {
            System.out.println("Failed to get field");
            return false;
        }
    }

    private boolean handleReady() throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("You are not in a game");
            return false;
        }
        
        if (!session.shipsReady) {
            System.out.println("You need to place ships first");
            return false;
        }
        
        MessageReadyToPlay result = server.readyToPlay(
            new MessageReadyToPlay(session.userNickName, session.currentGameSessionID));
        
        System.out.println("You're ready!");
        session.playerReady = true;
        if(result != null)
        {
            onReadyToPlay(result);
        }
        return true;
    }

    private boolean handleMove(String[] args) throws RemoteException {
        if (!session.yourTurn) {
            System.out.println("Not your turn! Wait for opponent's move.");
            return false;
        }
        
        int x, y;
        
        if (args.length >= 2) {
            try {
                x = Integer.parseInt(args[0]);
                y = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid coordinates");
                return false;
            }
        } else {
            System.out.print("Enter X coordinate (1-10): ");
            x = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter Y coordinate (1-10): ");
            y = Integer.parseInt(scanner.nextLine().trim());
        }
        
        if (x < 1 || x > 10 || y < 1 || y > 10) {
            System.out.println("x Coordinates must be between 1 and 10");
            return false;
        }
        
        MessageMoveResult result = server.move(
            new MessageMove(session.userNickName, session.currentGameSessionID, x, y));
        
        if (result != null && !result.Error()) {
            System.out.println("Move sent: (" + x + ", " + y + ")");
            onMoveResult(result);
            
            if (result.getGameOver()) {
                System.out.println("\nGAME OVER! You " + 
                    (result.getMessage().contains(session.userNickName) ? "WON!" : "LOST!"));
                session.resetGame();
            }
            
            return true;
        } else {
            System.out.println("Failed to make move: " + 
                (result != null ? result.getMessage() : "Unknown error"));
            return false;
        }
    }

    private boolean handleForfeit() throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("You are not in a game");
            return false;
        }
        
        System.out.print("Are you sure you want to forfeit? (Y/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("Forfeit cancelled");
            return false;
        }
        
        MessageGameOver result = server.forfeit(
            new MessageForfeit(session.userNickName, session.currentGameSessionID));
        
        if (result != null) {
            System.out.println("Game forfeited. You lose.");
            session.resetGame();
            return true;
        } else {
            System.out.println("Failed to forfeit game");
            return false;
        }
    }

    private boolean handleDisconnect() throws RemoteException {
        System.out.println("Are you sure you want to disconnect? (Y/N)");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("Disconnect cancelled");
            return false;
        }
        
        if (session.connected) {
            server.disconnect(new MessageDisconnect(session.userNickName));
            session.connected = false;
        }
        
        running = false;
        System.out.println("Disconnected from server. Goodbye!");
        return true;
    }

    private void printHelp() {
        System.out.println("COMMANDS:\n");
        
        System.out.println("LOBBY COMMANDS:");
        System.out.println("  help                  - Show this help");
        System.out.println("  ping                  - Test server connection");
        System.out.println("  users                 - List online players");
        System.out.println("  challenge <nick>      - Challenge a player");
        System.out.println("  answer                - Answer pending challenge");
        System.out.println("  listch                - List pending challenges");
        System.out.println("  disconnect/quit       - Disconnect from server\n");
        
        System.out.println("GAME COMMANDS (when in game):");
        System.out.println("  place [random/manual] - Place ships (random by default)");
        System.out.println("  field                 - View your battlefield");
        System.out.println("  ready                 - Mark yourself ready");
        System.out.println("  move <x> <y>          - Make a move");
        System.out.println("  forfeit               - Forfeit current game");
    }
    
    public void onChallengeRequest(MessageChallengeRequest req) {
        System.out.println("You've received new challenge " + req.getChallengeId() + " from " + req.getFrom());
        System.out.println("Type <answer> to answer");
        
        session.challenges.add(new ClientSession.ChallengeFromPlayer(
            req.getChallengeId(), req.getFrom()));
    }

    public void onGameStart(MessageGameStart msg) {
        System.out.println("Game started");
        System.out.println("Opponent:   " + msg.getOppNic());
        System.out.println("Session ID: " + msg.getSessionId());
        System.out.println("Print <place> to place ships"); 
        
        session.userOpponentNick = msg.getOppNic();
        session.currentGameSessionID = msg.getSessionId();
        session.gameStarted = false;
        session.shipsReady = false;
        session.playerReady = false;
        session.yourTurn = false;
    }

    public void onMoveResult(MessageMoveResult msg) {        
        boolean isOurMove = msg.getMessage().contains(session.userNickName);
        
        if (isOurMove) {
            System.out.println("Move: " + msg.getX() + " "+ msg.getY());
            System.out.println("Result: " + (msg.getHitted() ? "HIT!" : "MISS"));
            
            if (msg.getSunked()) {
                System.out.println(" SHIP SUNK!");
            }
            
            session.yourTurn = msg.getHitted(); // Hit = extra turn
            
            if (msg.getEnemyField() != null) {
                System.out.println("\nEnemy battlefield:");
                ClientSession.printField(msg.getEnemyField());
            }
        } else {
            System.out.println("Opponent's move: " + msg.getX() + " " + msg.getY());
            System.out.println("Result: " + (msg.getHitted() ? "HIT!" : "MISS"));
            
            if (msg.getSunked()) {
                System.out.println(" YOUR SHIP SUNK!");
            }
            
            session.yourTurn = !msg.getHitted(); // Opponent miss = our turn
        }
        
        if (session.yourTurn) {
            System.out.println("IT'S YOUR TURN!");
            System.out.println("Type <move> to attack");
        } else {
            System.out.println("Opponent's turn");
        }
    }

    public void onGameOver(MessageGameOver msg) {
        System.out.println("Game over!");

        if (msg.getWinnerNic().equals(session.userNickName)) {
            System.out.println("YOU WON! ");
        } else {
            System.out.println("YOU LOST");
            System.out.println("Winner: " + msg.getWinnerNic());
        }
        
        session.resetGame();
    }

    public void onError(MessageError msg) {
        System.out.println("Server error:");
        System.out.println(msg.getMessage());
    }

    public void onReadyToPlay(MessageReadyToPlay msg) {
        System.out.println("Game starts!"); 
        
        String starter = msg.getFrom();
        if (starter.equals(session.userNickName)) {
            System.out.println("You start first! ");
            session.yourTurn = true;
            System.out.println("Type <move> to make your move");
        } else {
            System.out.println(starter + " starts first");
            session.yourTurn = false;
            System.out.println("Opponent's move");
        }
        
        session.gameStarted = true;
    }

    public void onOpponentReadyToPlay(MessageOpponentReady msg) {
        System.out.println(msg.getFrom() + " is ready to play");
        
        if (session.playerReady) {
            System.out.println("Both players ready - game starting soon...");
        } else {
            System.out.println("Waiting for you to get ready...");
            System.out.println("Type <ready> when you're prepared");
        }
    }

    public void onFieldUpdate(MessageGetFieldResult msg) {
        System.out.println("Your current battlefield:");
        ClientSession.printField(msg.getField());
    }
}