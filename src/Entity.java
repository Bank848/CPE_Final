// Entity.java
/** คลาสแทนผู้เล่นหรือมอนสเตอร์ */
public class Entity {
    public String name;
    public int hp, maxHp;
    public int buffAttack;
    private boolean shieldActive = false;    
    public Entity(String name, int maxHp) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.buffAttack = 0;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void setShield(boolean v) { shieldActive = v; }
    public boolean isShieldActive()   { return shieldActive; }    
}
