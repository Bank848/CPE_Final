// Tile.java
/** แทนตัวอักษรบนกริด พร้อมคะแนน Black/Blue */
public class Tile {
    public char letter;
    public int blackPts;
    public int bluePts;

    public Tile(char letter, int blackPts, int bluePts) {
        this.letter = letter;
        this.blackPts = blackPts;
        this.bluePts = bluePts;
    }
}
