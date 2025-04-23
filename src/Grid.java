// Grid.java
import java.util.*;
public class Grid {
    private Tile[][] grid;
    private int size; private Random rand = new Random();
    private static final Set<Character> VOWELS = Set.of('A','E','I','O','U');
    private static final Set<Character> HARD = Set.of('Z','X','Q','J','K');

    public Grid(int size) {
        this.size = size; grid = new Tile[size][size]; generateRandomGrid();
    }

    protected void generateRandomGrid() {
        for (int r=0; r<size; r++) for (int c=0; c<size; c++) {
            char ch = (char)('A'+rand.nextInt(26));
            boolean special = rand.nextDouble()<0.05; // 5% chance
            int bp, lp;
            if (special) { bp = lp = rand.nextInt(3)+5; }
            else if (VOWELS.contains(ch)) { bp = rand.nextInt(3)+1; lp = rand.nextInt(3)+1; }
            else if (HARD.contains(ch)) { bp = rand.nextInt(5)+3; lp = rand.nextInt(5)+3; }
            else { bp = rand.nextInt(5)+1; lp = rand.nextInt(5)+1; }
            grid[r][c] = new Tile(ch, bp, lp, special);
        }
    }

    public Tile getTile(int r,int c){return grid[r][c];}
    public void removeAndCollapse(List<Position> positions){
        for (int c=0;c<size;c++){ Queue<Tile> q=new LinkedList<>();
            for (int r=size-1;r>=0;r--) if (!positions.contains(new Position(r,c))) q.add(grid[r][c]);
            for (int r=size-1;r>=0;r--) grid[r][c]= q.isEmpty()? refillTile(): q.poll();
        }}
    private Tile refillTile(){ char ch=(char)('A'+rand.nextInt(26)); boolean special=false; int bp,lp;
        if (VOWELS.contains(ch)){bp=rand.nextInt(3)+1; lp=rand.nextInt(3)+1;}
        else if (HARD.contains(ch)){bp=rand.nextInt(5)+3; lp=rand.nextInt(5)+3;}
        else {bp=rand.nextInt(5)+1; lp=rand.nextInt(5)+1;}
        return new Tile(ch,bp,lp,special);
    }
    public void shuffle(){generateRandomGrid();}
}
