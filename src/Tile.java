// Tile.java
public class Tile {
    public char letter;
    private int dmgPts, gemPts;
    private boolean special;

    // ① new field
    private GemType gemType = GemType.NONE;

    // ② original constructor — default to NONE
    public Tile(char letter, int dmgPts, int gemPts, boolean special) {
        this(letter, dmgPts, gemPts, special, GemType.NONE);
    }

    // ③ overload constructor to set gemType
    public Tile(char letter, int dmgPts, int gemPts, boolean special, GemType gemType) {
        this.letter   = letter;
        this.dmgPts   = dmgPts;
        this.gemPts   = gemPts;
        this.special  = special;
        this.gemType  = gemType;
    }

    public int getDmgPts()    { return dmgPts; }
    public int getGemPts()    { return gemPts; }
    public boolean isSpecial(){ return special; }

    // ④ getter + setter for gemType
    public GemType getGemType() {
        return gemType;
    }
    public void setGemType(GemType gemType) {
        this.gemType = gemType;
    }
}
