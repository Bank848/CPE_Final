// Grid.java
import java.util.*;

public class Grid {
    private Tile[][] grid;
    private int size;
    private Random rand = new Random();

    public Grid(int size) {
        this.size = size;
        grid = new Tile[size][size];
        generateRandomGrid();
    }

    // เปลี่ยนเป็น protected เพื่อให้เรียกจากภายนอกได้
    protected void generateRandomGrid() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                char ch = (char) ('A' + rand.nextInt(26));
                int bp = 1 + rand.nextInt(5);
                int lp = 1 + rand.nextInt(3);
                grid[r][c] = new Tile(ch, bp, lp);
            }
        }
    }

    public Tile getTile(int r, int c) {
        return grid[r][c];
    }

    public int getSize() {
        return size;
    }

    public void removeAndCollapse(List<Position> positions) {
        for (int c = 0; c < size; c++) {
            Queue<Tile> q = new LinkedList<>();
            for (int r = size - 1; r >= 0; r--) {
                if (!positions.contains(new Position(r, c))) {
                    q.add(grid[r][c]);
                }
            }
            for (int r = size - 1; r >= 0; r--) {
                if (!q.isEmpty()) grid[r][c] = q.poll();
                else {
                    char ch = (char) ('A' + rand.nextInt(26));
                    int bp = 1 + rand.nextInt(5);
                    int lp = 1 + rand.nextInt(3);
                    grid[r][c] = new Tile(ch, bp, lp);
                }
            }
        }
    }

    /** ฟังก์ชันใหม่สำหรับสุ่มทั้งกระดาน */
    public void shuffle() {
        generateRandomGrid();
    }
}