package seaBattle.gameLogic;

import java.lang.reflect.Array;
import java.security.InvalidParameterException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import seaBattle.gameLogic.Ship.Orientation;

interface Field {
    static final int EMPTY = 0;
    static final int SHIP = 1;
    static final int MISS = 2;
    static final int HITTED = 3;
    static final int SUNKED = 4;
    static final int CHECK = 5;
}

public class Player implements Field {
    private String userNic;

    private List<Ship> ships;
    private int[][] field;

    public Player(String nic) {
        userNic = nic;
        field = createRandomField();
    }

    public Player(String nic, List<Ship> ships) {
        checkShips(ships);
        userNic = nic;
        this.ships = ships;
        field = createField(ships);
    }

    public static boolean checkShips(List<Ship> ships) {
        int[][] f = new int[12][12];
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                f[i][j] = 0;
            }
        }
        int count1 = 0;
        int count2 = 0;
        int count3 = 0;
        int count4 = 0;
        if (ships == null || ships.size() != 10) {
            throw new InvalidParameterException("Wrong number of ships");
        }
        for (Ship s : ships) {
            switch (s.getLength()) {
                case 1:
                    count1++;
                    break;
                case 2:
                    count2++;
                    break;
                case 3:
                    count3++;
                    break;
                case 4:
                    count4++;
                    break;
                default:
                    throw new InvalidParameterException("Unknown ship type");
            }

            if (count1 > 4 || count2 > 3 || count3 > 2 || count4 > 1) {
                throw new InvalidParameterException("Wrong number of ships");
            }

            List<int[]> que = new ArrayList<>();

            for (int i = 0; i < s.getLength(); i++) {
                int x;
                int y;
                if (s.getOrientation() == Orientation.horizontal) {
                    x = s.getX() + i;
                    y = s.getY();
                } else {
                    x = s.getX();
                    y = s.getY() + i;
                }

                if (x < 0 || x >= 12 || y < 0 || y >= 12) {
                    throw new InvalidParameterException("Wrong ship placement (out of bounds)");
                }

                if (f[y][x] != 0) {
                    throw new InvalidParameterException("Wrong ship placement");
                }

                f[y][x] = 1;

                que.add(new int[] { x - 1, y - 1 });
                que.add(new int[] { x - 1, y });
                que.add(new int[] { x - 1, y + 1 });
                que.add(new int[] { x, y - 1 });
                que.add(new int[] { x, y + 1 });
                que.add(new int[] { x + 1, y - 1 });
                que.add(new int[] { x + 1, y });
                que.add(new int[] { x + 1, y + 1 });
            }
            for (int[] coord : que) {
                int cx = coord[0];
                int cy = coord[1];
                if (cx >= 0 && cx < 12 && cy >= 0 && cy < 12) {
                    if (f[cy][cx] == 0) {
                        f[cy][cx] = 1;
                    }
                }
            }
        }
        return true;
    }

    private int[][] createField(List<Ship> ships) {
        int[][] resField = new int[12][12];
        for (int i = 0; i < resField.length; i++) {
            for (int j = 0; j < resField.length; j++) {
                resField[i][j] = EMPTY;
            }
        }
        for (Ship s : ships) {
            for (int i = 0; i < s.getLength(); i++) {
                int x;
                int y;
                if (s.getOrientation() == Orientation.horizontal) {
                    x = s.getX() + i;
                    y = s.getY();
                } else {
                    x = s.getX();
                    y = s.getY() + i;
                }

                if (y >= 0 && y < resField.length && x >= 0 && x < resField.length) {
                    resField[y][x] = SHIP;
                }
            }
        }
        return resField;
    }

    private int[][] createRandomField() {
        int[][] resField = new int[12][12];
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                resField[i][j] = EMPTY;
            }
        }
        return resField;
    }

    public String getNic() {
        return userNic;
    }

    public List<Ship> getShips() {
        return ships;
    }

    public int[][] getField() {
        return field;
    }

    public void setPlaceShips(List<Ship> ships) {
        checkShips(ships);
        this.ships = ships;
        field = createField(ships);
    }

    public class MoveResult {
        public boolean hitted;
        public boolean sunked;
        public boolean gameOver;
        public int[][] field;

        public MoveResult(boolean hitted, boolean sunked, boolean gameOver, int[][] field) {
            this.hitted = hitted;
            this.sunked = sunked;
            this.gameOver = gameOver;
            this.field = field;
        }
    }

    public MoveResult move(int x, int y) {
        boolean hitted = false;
        boolean sunked = false;
        boolean gameOver = false;

        if (y < 0 || y >= field.length || x < 0 || x >= field.length) {
            return new MoveResult(false, false, false, getSafeField(field));
        }

        if (field[y][x] != SHIP) {
            if (field[y][x] == EMPTY) {
                field[y][x] = MISS;
            }
            return new MoveResult(hitted, sunked, gameOver, getSafeField(field));
        }
        hitted = true;
        field[y][x] = HITTED;

        sunked = checkIsSunked(x, y);

        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field.length; j++) {
                if (field[i][j] == CHECK) {
                    field[i][j] = HITTED;
                }
            }
        }

        gameOver = sunked;
        if (sunked) {
            for (int i = 0; i < field.length; i++) {
                for (int j = 0; j < field.length; j++) {
                    if (field[i][j] == SHIP) {
                        gameOver = false;
                        return new MoveResult(hitted, sunked, gameOver, getSafeField(field));
                    }
                }
            }
        }
        MoveResult res = new MoveResult(hitted, sunked, gameOver, getSafeField(field));
        return res;
    }

    private boolean checkIsSunked(int x, int y) {
        Queue<int[]> q = new ArrayDeque<>();
        List<int[]> toChange = new ArrayList<>();
        q.add(new int[] { x, y });
        toChange.add(new int[] { x, y });
        field[y][x] = CHECK;

        while (!q.isEmpty()) {
            int[] el = q.poll();
            int cx = el[0];
            int cy = el[1];

            if (cy - 1 >= 0 && field[cy - 1][cx] == SHIP) return false;
            if (cy + 1 < field.length && field[cy + 1][cx] == SHIP) return false;
            if (cx - 1 >= 0 && field[cy][cx - 1] == SHIP) return false;
            if (cx + 1 < field.length && field[cy][cx + 1] == SHIP) return false;

            if (cy - 1 >= 0 && field[cy - 1][cx] == HITTED) {
                q.add(new int[] { cx, cy - 1 });
                toChange.add(new int[] { cx, cy - 1 });
                field[cy - 1][cx] = CHECK;
            }
            if (cy + 1 < field.length && field[cy + 1][cx] == HITTED) {
                q.add(new int[] { cx, cy + 1 });
                toChange.add(new int[] { cx, cy + 1 });
                field[cy + 1][cx] = CHECK;
            }
            if (cx - 1 >= 0 && field[cy][cx - 1] == HITTED) {
                q.add(new int[] { cx - 1, cy });
                toChange.add(new int[] { cx - 1, cy });
                field[cy][cx - 1] = CHECK;
            }
            if (cx + 1 < field.length && field[cy][cx + 1] == HITTED) {
                q.add(new int[] { cx + 1, cy });
                toChange.add(new int[] { cx + 1, cy });
                field[cy][cx + 1] = CHECK;
            }
        }
        for(int[] point : toChange)
        {
            field[point[1]][point[0]] = SUNKED;
        }
        return true;
    }

    private int[][] getSafeField(int[][] field) {
        int[][] res = new int[12][12];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res.length; j++) {
                res[i][j] = (field[i][j] == SHIP) ? EMPTY : field[i][j];
            }
        }
        return res;
    }

    public void clearField() {
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field.length; j++) {
                field[i][j] = EMPTY;
            }
        }
    }
}
