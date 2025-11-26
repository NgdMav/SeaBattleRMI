package seaBattle.gameLogic;

import java.util.List;

import seaBattle.gameLogic.Player.MoveResult;

public class GameSession {
    private Player A;
    private boolean aready;
    private Player B;
    private boolean bready;
    private long sessionId;
    boolean gameRuns;

    private String currentTurnNic;

    public GameSession(long sessionId, String playerANic, String playerBNic, List<Ship> playerAShips,
            List<Ship> playerBShips) {
        this.sessionId = sessionId;
        if (playerAShips == null) {
            A = new Player(playerANic);
        } else {
            A = new Player(playerANic, playerAShips);
        }
        if (playerBShips == null) {
            B = new Player(playerBNic);
        } else {
            B = new Player(playerBNic, playerBShips);
        }
        gameRuns = false;
        aready = false;
        bready = false;
        currentTurnNic = null;
    }

    public void gameStart() {
        gameRuns = true;
        currentTurnNic = A.getNic();
        System.out.println("Game started: first move = " + currentTurnNic);
    }

    public void gameEnd() {
        gameRuns = false;
        aready = false;
        bready = false;
        currentTurnNic = null;
        A.clearField();
        B.clearField();
    }

    public long getSessionId() {
        return sessionId;
    }

    public Player getPlayerA() {
        return A;
    }

    public Player getPlayerB() {
        return B;
    }

    public synchronized boolean setPlaceShip(String nic, List<Ship> ships) {
        if (gameRuns) {
            return false;
        }
        if (A.getNic().equals(nic)) {
            A.setPlaceShips(ships);
        } else if (B.getNic().equals(nic)) {
            B.setPlaceShips(ships);
        } else {
            // throw ex;
            return false;
        }
        return true;
    }

    public synchronized boolean playerAReady(String nic) {
        if (A.getNic().equals(nic)) {
            aready = true;
            return true;
        }
        return false;
    }
    public synchronized boolean playerBReady(String nic) {
        if (B.getNic().equals(nic)) {
            bready = true;
            return true;
        }
        return false;
    }

    public synchronized boolean playerReady(String nic) {
        if (A.getNic().equals(nic)) {
            aready = true;
        } else if (B.getNic().equals(nic)) {
            bready = true;
        } else {
            // throw ex;
            return false;
        }
        if (aready && bready) {
            gameStart();
            return true;
        }
        return false;
    }

    public synchronized MoveResult move(String nic, int x, int y) {
        if (!gameRuns)
            throw new IllegalStateException("Game not started yet!");
        if (!nic.equals(currentTurnNic)) {
            throw new IllegalStateException("Not your turn, " + nic);
        }

        MoveResult res;
        if (A.getNic().equals(nic)) {
            res = B.move(x, y);
        } else if (B.getNic().equals(nic)) {
            res = A.move(x, y);
        } else {
            res = A.new MoveResult(false, false, false, new int[12][12]);
        }

        if (!res.gameOver) {
            if (!res.hitted) {
                currentTurnNic = getEnemyNic(nic);
            }
        } else {
            gameEnd();
        }

        return res;
    }

    public synchronized String getCurrentTurnNic() {
        return currentTurnNic;
    }

    public String getEnemyNic(String from) {
        if (A.getNic().equals(from)) {
            return B.getNic();
        }
        if (B.getNic().equals(from)) {
            return A.getNic();
        }
        return "";
    }

    public int[][] getField(String nic) {
        if (A.getNic().equals(nic)) {
            return A.getField();
        } else if (B.getNic().equals(nic)) {
            return B.getField();
        }

        return null;
    }

    public String getToStart() {
        return currentTurnNic;
    }
}
