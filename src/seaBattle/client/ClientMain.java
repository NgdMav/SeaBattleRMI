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
        System.out.println("==============================================");
        System.out.println("=      Sea Battle RMI Client v1.0           =");
        System.out.println("==============================================");
        
        System.out.println("\nConnecting to server at " + host + ":" + Protocol.PORT + "...");
        
        try {
            String url = "rmi://" + host + ":" + Protocol.PORT + "/SeaBattleService";
            server = (SeaBattleService) Naming.lookup(url);
            System.out.println("! Connected to RMI registry");
        } catch (Exception e) {
            System.err.println("x Cannot connect to server: " + e.getMessage());
            return;
        }

        System.out.println("Registering client callback...");
        ClientCallbackImpl callback = new ClientCallbackImpl(this);

        System.out.println("Authenticating as " + session.userNickName + "...");
        MessageConnect req = new MessageConnect(session.userNickName, session.userFullName, callback);
        MessageConnectResult res = server.connect(req);

        if (!res.Error()) {
            System.out.println("! Authentication successful!");
            session.connected = true;
        } else {
            System.out.println("x Authentication failed: " + res.getMessage());
            return;
        }

        System.out.println("\n" + getStatusBanner());
        System.out.println("\nType 'help' for available commands");
        
        commandLoop();
    }

    private void commandLoop() {
        while (running && session.connected) {
            try {
                printPrompt();
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();
                String[] args = parts.length > 1 ? 
                    java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                
                if (!processCommand(command, args)) {
                    System.out.println("Unknown command. Type 'help' for list of commands.");
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
            case "users", "u" -> handleUsers();
            case "challenge", "ch" -> handleChallenge(args);
            case "answer", "a2ch" -> handleAnswerChallenge(args);
            case "listch" -> handleListChallenges();
            case "place", "plsh" -> handlePlaceShips(args);

            // case "manual":
            //    return handleManualPlacement();

            case "field", "gf" -> handleGetField();
            case "ready", "r" -> handleReady();
            case "move", "m" -> handleMove(args);
            case "forfeit", "ff" -> handleForfeit();
            case "status" -> handleStatus();
            case "disconnect", "quit", "exit" -> handleDisconnect();
            default -> false;
        };
    }

    private boolean handlePing() throws RemoteException {
        MessagePong pong = server.ping(new MessagePing());
        if (pong != null) {
            System.out.println("! Server responded - connection is active");
            return true;
        }
        return false;
    }

    private boolean handleUsers() throws RemoteException {
        MessageUserResult users = server.userList(new MessageUser());
        if (users.Error()) {
            System.out.println("x Error getting user list: " + users.getMessage());
            return false;
        }
        
        String[] userList = users.getNics();
        System.out.println("\n----------------------------------------");
        System.out.println("          ONLINE PLAYERS (" + userList.length + ")");
        System.out.println("----------------------------------------");
        
        for (String user : userList) {
            if (user.equals(session.userNickName)) {
                System.out.println("  ▶ " + user + " (you)");
            } else {
                System.out.println("  • " + user);
            }
        }
        
        if (userList.length == 1) {
            System.out.println("\n  No other players online. Wait for someone to connect.");
        }
        System.out.println("----------------------------------------");
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
            System.out.println("x Opponent nickname cannot be empty");
            return false;
        }
        
        if (opponent.equals(session.userNickName)) {
            System.out.println("x You cannot challenge yourself");
            return false;
        }
        
        MessageChallengeSuccessfullySend result = 
            server.createChallenge(new MessageChallenge(session.userNickName, opponent));
        
        if (result != null) {
            System.out.println("! Challenge sent to " + opponent + " (ID: " + result.getSessionId() + ")");
            return true;
        } else {
            System.out.println("x Failed to send challenge to " + opponent);
            return false;
        }
    }

    private boolean handleAnswerChallenge(String[] args) throws RemoteException {
        if (session.challenges.isEmpty()) {
            System.out.println("x No pending challenges");
            return false;
        }
        
        long challengeId;
        boolean accepted;
        
        if (args.length >= 2) {
            try {
                challengeId = Long.parseLong(args[0]);
                accepted = args[1].equalsIgnoreCase("yes") || args[1].equalsIgnoreCase("y");
            } catch (NumberFormatException e) {
                System.out.println("x Invalid challenge ID format");
                return false;
            }
        } else {
            System.out.println("\n----------------------------------------");
            System.out.println("         PENDING CHALLENGES");
            System.out.println("----------------------------------------");
            
            for (ClientSession.ChallengeFromPlayer cfp : session.challenges) {
                System.out.println("  ID: " + cfp.challengeID() + " | From: " + cfp.playerNickName());
            }
            
            System.out.print("\nEnter challenge ID: ");
            challengeId = Long.parseLong(scanner.nextLine().trim());
            
            System.out.print("Accept challenge? (yes/no): ");
            String response = scanner.nextLine().trim().toLowerCase();
            accepted = response.equals("yes") || response.equals("y");
        }
        
        MessageChallengeResult result = server.answerChallenge(
            new MessageChallengeResponse(challengeId, accepted));
        
        if (result != null && !result.Error()) {
            if (accepted) {
                System.out.println("! Challenge accepted!");
            } else {
                System.out.println("! Challenge declined");
            }
            
            session.challenges.removeIf(c -> c.challengeID() == challengeId);
            return true;
        } else {
            System.out.println("x Failed to answer challenge: " + 
                (result != null ? result.getMessage() : "Unknown error"));
            return false;
        }
    }

    private boolean handleListChallenges() {
        if (session.challenges.isEmpty()) {
            System.out.println("No pending challenges");
            return true;
        }
        
        System.out.println("\n----------------------------------------");
        System.out.println("         PENDING CHALLENGES");
        System.out.println("----------------------------------------");
        
        for (ClientSession.ChallengeFromPlayer cfp : session.challenges) {
            System.out.println("  Challenge ID: " + cfp.challengeID());
            System.out.println("  From: " + cfp.playerNickName());
            System.out.println("  --------------------------------------");
        }
        
        System.out.println("\nUse: answer <id> <yes/no>  to respond");
        System.out.println("----------------------------------------");
        return true;
    }

    private boolean handlePlaceShips(String[] args) throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("x You are not in a game");
            return false;
        }
        
        boolean useRandom = args.length <= 0 || !args[0].equalsIgnoreCase("manual");

        List<Ship> ships;
        
        if (useRandom) {
            System.out.println("\nGenerating random ship placement...");
            ships = ClientSession.randomShips();
            System.out.println("! Ships generated successfully");
            
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
            System.out.println("! Ships placed successfully!");
            session.shipsReady = true;
            return true;
        } else {
            System.out.println("x Failed to place ships: " + result.getMessage());
            return false;
        }
    }

    private List<Ship> handleManualShipPlacement() {
        System.out.println("\n----------------------------------------");
        System.out.println("        MANUAL SHIP PLACEMENT");
        System.out.println("----------------------------------------");
        System.out.println("Format: x y length orientation");
        System.out.println("  x, y: coordinates (1-10)");
        System.out.println("  length: ship length (1-4)");
        System.out.println("  orientation: h (horizontal) or v (vertical)");
        System.out.println("\nExample: 3 5 3 h  (3-length ship at (3,5) horizontal)");
        System.out.println("Type 'done' when finished, 'cancel' to abort");
        System.out.println("----------------------------------------\n");
        
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
                    System.out.println("x Invalid format. Use: x y length h/v");
                    continue;
                }
                
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int length = Integer.parseInt(parts[2]);
                    boolean vertical = parts[3].equals("v");
                    
                    if (length != shipCounts[i]) {
                        System.out.println("x This ship must be length " + shipCounts[i]);
                        continue;
                    }
                    
                    Ship ship = new Ship(x, y, length, vertical);
                    
                    // Check if ship can be placed
                    if (!ClientSession.canPlace(field, ship)) {
                        System.out.println("x Cannot place ship there (overlap or out of bounds)");
                        continue;
                    }
                    
                    ships.add(ship);
                    ClientSession.placeOnField(field, ship);
                    
                    System.out.println("! Ship placed at (" + x + "," + y + ") " + 
                        (vertical ? "vertical" : "horizontal"));
                    break;
                    
                } catch (Exception e) {
                    System.out.println("x Invalid input: " + e.getMessage());
                }
            }
        }
        
        System.out.println("\n! All ships placed successfully!");
        System.out.println("\nFinal battlefield:");
        ClientSession.printField(field);
        
        return ships;
    }

    private boolean handleGetField() throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("x You are not in a game");
            return false;
        }
        
        MessageGetFieldResult result = server.getField(
            new MessageGetField(session.userNickName, session.currentGameSessionID));
        
        if (result != null && result.getField() != null) {
            System.out.println("\n----------------------------------------");
            System.out.println("           YOUR BATTLEFIELD");
            System.out.println("----------------------------------------");
            ClientSession.printField(result.getField());
            return true;
        } else {
            System.out.println("x Failed to get field");
            return false;
        }
    }

    private boolean handleReady() throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("x You are not in a game");
            return false;
        }
        
        if (!session.shipsReady) {
            System.out.println("x You need to place ships first");
            return false;
        }
        
        MessageReadyToPlay result = server.readyToPlay(
            new MessageReadyToPlay(session.userNickName, session.currentGameSessionID));
        
        if (result != null) {
            System.out.println("! Ready status sent to server");
            session.playerReady = true;
            return true;
        } else {
            System.out.println("x Failed to send ready status");
            return false;
        }
    }

    private boolean handleMove(String[] args) throws RemoteException {
        if (!session.yourTurn) {
            System.out.println("x Not your turn! Wait for opponent's move.");
            return false;
        }
        
        int x, y;
        
        if (args.length >= 2) {
            try {
                x = Integer.parseInt(args[0]);
                y = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("x Invalid coordinates");
                return false;
            }
        } else {
            System.out.println("\n----------------------------------------");
            System.out.println("              MAKE A MOVE");
            System.out.println("----------------------------------------");
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
            System.out.println("! Move sent: (" + x + ", " + y + ")");
            
            if (result.getGameOver()) {
                System.out.println("\n🎉 GAME OVER! You " + 
                    (result.getMessage().contains(session.userNickName) ? "WON!" : "LOST!"));
                session.resetGame();
            }
            
            return true;
        } else {
            System.out.println("x Failed to make move: " + 
                (result != null ? result.getMessage() : "Unknown error"));
            return false;
        }
    }

    private boolean handleForfeit() throws RemoteException {
        if (session.currentGameSessionID == null) {
            System.out.println("x You are not in a game");
            return false;
        }
        
        System.out.print("Are you sure you want to forfeit? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (!confirm.equals("yes") && !confirm.equals("y")) {
            System.out.println("Forfeit cancelled");
            return false;
        }
        
        MessageGameOver result = server.forfeit(
            new MessageForfeit(session.userNickName, session.currentGameSessionID));
        
        if (result != null) {
            System.out.println("! Game forfeited. You lose.");
            session.resetGame();
            return true;
        } else {
            System.out.println("x Failed to forfeit game");
            return false;
        }
    }

    private boolean handleStatus() {
        System.out.println(getStatusBanner());
        return true;
    }

    private boolean handleDisconnect() throws RemoteException {
        System.out.print("Are you sure you want to disconnect? (yes/no): ");
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

    private void printPrompt() {
        String prompt = "\n";
        
        if (!session.connected) {
            prompt += "[DISCONNECTED] ";
        } else if (session.currentGameSessionID != null) {
            prompt += "[" + session.userOpponentNick + "] ";
            
            if (!session.shipsReady) {
                prompt += "[SHIPS] ";
            } else if (!session.playerReady) {
                prompt += "[READY] ";
            } else if (session.yourTurn) {
                prompt += "[YOUR TURN] ";
            } else {
                prompt += "[OPPONENT'S TURN] ";
            }
        } else {
            prompt += "[LOBBY] ";
        }
        
        if (!session.challenges.isEmpty()) {
            prompt += "[CHALLENGES: " + session.challenges.size() + "] ";
        }
        
        prompt += "> ";
        System.out.print(prompt);
    }

    private String getStatusBanner() {
        StringBuilder sb = new StringBuilder();
        sb.append("----------------------------------------\n");
        sb.append("           CLIENT STATUS\n");
        sb.append("----------------------------------------\n");
        sb.append("Player:      ").append(session.userNickName).append("\n");
        sb.append("Status:      ").append(session.connected ? "Connected !" : "Disconnected x").append("\n");
        
        if (session.currentGameSessionID != null) {
            sb.append("\n--- ACTIVE GAME ---\n");
            sb.append("Opponent:    ").append(session.userOpponentNick).append("\n");
            sb.append("Session ID:  ").append(session.currentGameSessionID).append("\n");
            sb.append("Ships ready: ").append(session.shipsReady ? "Yes !" : "No x").append("\n");
            sb.append("Player ready:").append(session.playerReady ? "Yes !" : "No x").append("\n");
            sb.append("Your turn:   ").append(session.yourTurn ? "Yes !" : "No x").append("\n");
        } else {
            sb.append("\n--- LOBBY ---\n");
            sb.append("No active game\n");
        }
        
        if (!session.challenges.isEmpty()) {
            sb.append("\n--- PENDING CHALLENGES ---\n");
            for (ClientSession.ChallengeFromPlayer cfp : session.challenges) {
                sb.append("ID: ").append(cfp.challengeID())
                  .append(" from ").append(cfp.playerNickName()).append("\n");
            }
        }
        
        sb.append("----------------------------------------");
        return sb.toString();
    }

    private void printHelp() {
        System.out.println("\n----------------------------------------");
        System.out.println("              COMMANDS");
        System.out.println("----------------------------------------\n");
        
        System.out.println("LOBBY COMMANDS:");
        System.out.println("  help                  - Show this help");
        System.out.println("  status                - Show current status");
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
        System.out.println("  status                - Show game status\n");
        
        System.out.println("SHORTCUTS:");
        System.out.println("  u        - users      r  - ready");
        System.out.println("  ch       - challenge  m  - move");
        System.out.println("  a2ch     - answer     ff - forfeit");
        System.out.println("  plsh     - place      gf - field");
        System.out.println("----------------------------------------");
    }

    // ==================== CALLBACK HANDLERS ====================
    
    public void onChallengeRequest(MessageChallengeRequest req) {
        System.out.println("\n============================================");
        System.out.println("    CHALLENGE REQUEST!");
        System.out.println("============================================");
        System.out.println("Player:    " + req.getFrom());
        System.out.println("Challenge: " + req.getChallengeId());
        System.out.println("============================================");
        System.out.println("Type 'answer' to respond");
        System.out.println("============================================\n");
        
        session.challenges.add(new ClientSession.ChallengeFromPlayer(
            req.getChallengeId(), req.getFrom()));
        
        printPrompt();
    }

    public void onGameStart(MessageGameStart msg) {
        System.out.println("\n============================================");
        System.out.println("         GAME STARTED!");
        System.out.println("============================================");
        System.out.println("Opponent:   " + msg.getOppNic());
        System.out.println("Session ID: " + msg.getSessionId());
        System.out.println("============================================");
        System.out.println("Type 'place' to position your ships");
        System.out.println("============================================\n");
        
        session.userOpponentNick = msg.getOppNic();
        session.currentGameSessionID = msg.getSessionId();
        session.gameStarted = false;
        session.shipsReady = false;
        session.playerReady = false;
        session.yourTurn = false;
        
        printPrompt();
    }

    public void onMoveResult(MessageMoveResult msg) {
        System.out.println("\n============================================");
        System.out.println("             MOVE RESULT");
        System.out.println("============================================");
        
        boolean isOurMove = msg.getMessage().contains(session.userNickName);
        
        if (isOurMove) {
            System.out.println("Your move at: (" + msg.getX() + ", " + msg.getY() + ")");
            System.out.println("Result: " + (msg.getHitted() ? "HIT! 💥" : "MISS 💧"));
            
            if (msg.getSunked()) {
                System.out.println(" SHIP SUNK!");
            }
            
            session.yourTurn = msg.getHitted(); // Hit = extra turn
            
            if (msg.getEnemyField() != null) {
                System.out.println("\nEnemy battlefield:");
                ClientSession.printField(msg.getEnemyField());
            }
        } else {
            System.out.println("Opponent moved at: (" + msg.getX() + ", " + msg.getY() + ")");
            System.out.println("Result: " + (msg.getHitted() ? "HIT!" : "MISS"));
            
            if (msg.getSunked()) {
                System.out.println(" YOUR SHIP SUNK!");
            }
            
            session.yourTurn = !msg.getHitted(); // Opponent miss = our turn
        }
        
        System.out.println("============================================");
        
        if (session.yourTurn) {
            System.out.println("IT'S YOUR TURN!");
            System.out.println("Type 'move x y' to attack");
        } else {
            System.out.println("⏳ Waiting for opponent's move...");
        }
        System.out.println("============================================\n");
        
        printPrompt();
    }

    public void onGameOver(MessageGameOver msg) {
        System.out.println("\n============================================");
        System.out.println("            GAME OVER!");
        System.out.println("============================================");
        
        if (msg.getWinnerNic().equals(session.userNickName)) {
            System.out.println("           YOU WON! ");
            System.out.println("============================================");
            System.out.println("Congratulations on your victory!");
        } else {
            System.out.println("           YOU LOST");
            System.out.println("============================================");
            System.out.println("Winner: " + msg.getWinnerNic());
            System.out.println("Better luck next time!");
        }
        
        System.out.println("============================================\n");
        
        session.resetGame();
        printPrompt();
    }

    public void onError(MessageError msg) {
        System.out.println("\n============================================");
        System.out.println("            SERVER ERROR");
        System.out.println("============================================");
        System.out.println(msg.getMessage());
        System.out.println("============================================\n");
        
        printPrompt();
    }

    public void onReadyToPlay(MessageReadyToPlay msg) {
        System.out.println("\n============================================");
        System.out.println("             GAME READY!");
        System.out.println("============================================");
        
        String starter = msg.getFrom();
        if (starter.equals(session.userNickName)) {
            System.out.println("You start first! ");
            session.yourTurn = true;
            System.out.println("Type 'move x y' to make your first move");
        } else {
            System.out.println(starter + " starts first");
            session.yourTurn = false;
            System.out.println("Waiting for opponent's move...");
        }
        
        session.gameStarted = true;
        System.out.println("============================================\n");
        
        printPrompt();
    }

    public void onOpponentReadyToPlay(MessageOpponentReady msg) {
        System.out.println("\n============================================");
        System.out.println("        OPPONENT READY!");
        System.out.println("============================================");
        System.out.println(msg.getFrom() + " is ready to play");
        
        if (session.playerReady) {
            System.out.println("Both players ready - game starting soon...");
        } else {
            System.out.println("Waiting for you to get ready...");
            System.out.println("Type 'ready' when you're prepared");
        }
        
        System.out.println("============================================\n");
        printPrompt();
    }

    public void onFieldUpdate(MessageGetFieldResult msg) {
        System.out.println("\n============================================");
        System.out.println("           FIELD UPDATE");
        System.out.println("============================================");
        System.out.println("Your current battlefield:");
        ClientSession.printField(msg.getField());
        System.out.println("============================================\n");
        printPrompt();
    }
}