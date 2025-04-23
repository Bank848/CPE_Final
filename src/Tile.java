    // Tile.java
    public class Tile {
        public char letter;
        private int dmgPts, gemPts;
        private boolean special;
        public Tile(char letter, int dmgPts, int gemPts, boolean special) {
            this.letter = letter; this.dmgPts = dmgPts; this.gemPts = gemPts; this.special = special;
        }
        public int getDmgPts() { return dmgPts; }
        public int getGemPts() { return gemPts; }
        public boolean isSpecial() { return special; }
    }