package seaBattle.client;

import seaBattle.gameLogic.Ship;
import seaBattle.protocol.messages.messagesResult.MessageMoveResult;

import java.util.ArrayList;
import java.util.List;

public class ClientSession {
    public boolean connected = false;
    public String userNickName;
    public String userFullName;
    public String userOpponentNick;

    public List<ChallengeFromPlayer> challenges = new ArrayList<>();

    public Long currentGameSessionID = null;
    public boolean shipsReady = false;
    public boolean gameStarted = false;
    public boolean playerReady = false;
    public boolean yourTurn = false;

    public ClientSession(String nick, String fullName) {
        this.userNickName = nick;
        this.userFullName = fullName;
    }

    public record ChallengeFromPlayer(long challengeID, String playerNickName) {
    }

    public void resetGame() {
        currentGameSessionID = null;
        gameStarted = false;
        shipsReady = false;
        playerReady = false;
        yourTurn = false;
    }

    // === METHODS MOVED FROM OLD CLIENT ===

    public static void printMove(MessageMoveResult msg, ClientSession ses) {
        int x = msg.getX();
        int y = msg.getY();
        boolean hit = msg.getHitted();
        boolean sunk = msg.getSunked();
        int[][] field = msg.getEnemyField();
        // boolean enemy = msg.getEnemy();

        if (hit)
            System.out.println("Hit at " + x + " " + y);
        else
            System.out.println("Miss at " + x + " " + y);

        if (sunk)
            System.out.println("Ship sunk!");

        printField(field);

        ses.yourTurn = !hit; // hit → move again
        if (ses.yourTurn)
            System.out.println("Your turn (type 'move')");
        else
            System.out.println("Opponent's turn...");
    }

    public static void printField(int[][] f) {
        System.out.print("   ");
        for (int x = 1; x <= 10; x++) System.out.print(x + " ");
        System.out.println();

        for (int y = 1; y <= 10; y++) {
            System.out.print((y < 10 ? " " : "") + y + " ");
            for (int x = 1; x <= 10; x++) {
                int c = f[y][x];
                switch (c) {
                    case 0 -> System.out.print(". ");
                    case 1 -> System.out.print("S ");
                    case 2 -> System.out.print("M ");
                    case 3 -> System.out.print("H ");
                    default -> System.out.print("? ");
                }
            }
            System.out.println();
        }
    }

    // random ship placement from your old code
    public static List<Ship> randomShips() {
        int[][] field = new int[12][12];
        List<Ship> result = new ArrayList<>();

        int[] sizes = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

        for (int len : sizes) {
            boolean placed = false;

            for (int attempt = 0; attempt < 500; attempt++) {
                boolean vert = Math.random() < 0.5;
                int x = 1 + (int)(Math.random() * 10);
                int y = 1 + (int)(Math.random() * 10);

                try {
                    Ship s = new Ship(x, y, len, vert);
                    if (!canPlace(field, s))
                        continue;

                    placeOnField(field, s);
                    result.add(s);
                    placed = true;
                    break;

                } catch (Exception ignore) {}
            }
            if (!placed)
                throw new RuntimeException("Random ship placement failed");
        }
        return result;
    }

    public static boolean canPlace(int[][] f, Ship s) {
        int x = s.getX(), y = s.getY(), len = s.getLength();
        boolean v = s.getOrientation() == Ship.Orientation.vertical;

        if (v && y + len - 1 > 10) return false;
        if (!v && x + len - 1 > 10) return false;

        for (int i = 0; i < len; i++) {
            int cx = x + (v ? 0 : i);
            int cy = y + (v ? i : 0);

            if (f[cy][cx] != 0) return false;

            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx >= 1 && nx <= 10 && ny >= 1 && ny <= 10)
                        if (f[ny][nx] != 0) return false;
                }
        }
        return true;
    }

    public static void placeOnField(int[][] f, Ship s) {
        int x = s.getX(), y = s.getY(), len = s.getLength();
        boolean v = s.getOrientation() == Ship.Orientation.vertical;

        for (int i = 0; i < len; i++) {
            int cx = x + (v ? 0 : i);
            int cy = y + (v ? i : 0);
            f[cy][cx] = 1;
        }
    }

    public static int[][] makeField(List<Ship> ships) {
        int[][] field = new int[12][12];
        for (Ship s : ships)
            placeOnField(field, s);
        return field;
    }
}
