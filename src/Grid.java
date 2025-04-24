import java.util.*;

public class Grid {
    private final Tile[][] grid;
    private final int size;
    private final Random rand = new Random();

    // ปรับความน่าจะเป็นให้สระเกิดขึ้นได้ง่ายขึ้น (30% เป็นสระ, 70% เป็นพยัญชนะ)
    private static final double VOWEL_PROB = 0.3;
    private static final char[] VOWEL_ARRAY = {'A','E','I','O','U'};
    private static final char[] CONSONANT_ARRAY = {
        'B','C','D','F','G','H','J','K','L','M','N','P','Q','R','S','T','V','W','X','Y','Z'
    };

    private static final Set<Character> VOWELS = Set.of('A','E','I','O','U');
    private static final Set<Character> HARD = Set.of('Z','X','Q','J','K');

    public Grid(int size) {
        this.size = size;
        grid = new Tile[size][size];
        generateRandomGrid();
    }

    protected void generateRandomGrid() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = createRandomTile();
            }
        }
    }

    // สุ่มสร้าง Tile ใหม่
    private Tile createRandomTile() {
        char ch = randomLetter();
        boolean special = rand.nextDouble() < 0.05;
        int bp, lp;
        if (special) {
            bp = lp = rand.nextInt(3) + 5;
        } else if (VOWELS.contains(ch)) {
            bp = rand.nextInt(3) + 1;
            lp = rand.nextInt(3) + 1;
        } else if (HARD.contains(ch)) {
            bp = rand.nextInt(5) + 3;
            lp = rand.nextInt(5) + 3;
        } else {
            bp = rand.nextInt(5) + 1;
            lp = rand.nextInt(5) + 1;
        }
        return new Tile(ch, bp, lp, special);
    }

    private char randomLetter() {
        if (rand.nextDouble() < VOWEL_PROB) {
            return VOWEL_ARRAY[rand.nextInt(VOWEL_ARRAY.length)];
        } else {
            return CONSONANT_ARRAY[rand.nextInt(CONSONANT_ARRAY.length)];
        }
    }

    public Tile getTile(int r, int c) {
        return grid[r][c];
    }

    /**
     * แทนที่เฉพาะตำแหน่งที่ผู้เล่นเลือก โดยตรง ไม่ทำการ collapse
     */
    public void removeAndCollapse(List<Position> positions) {
        for (Position pos : positions) {
            int row = pos.row;
            int col = pos.col;
            grid[row][col] = createRandomTile();
        }
    }

    public void shuffle() {
        generateRandomGrid();
    }
}
