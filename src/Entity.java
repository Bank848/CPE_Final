// Entity.java

public class Entity {
    public String name;
    public int hp, maxHp;
    public int buffAttack;
    public int baseDamage;      // พลังโจมตีพื้นฐานจากดาบ
    public int armor;           // ค่าพลังป้องกันลดความเสียหาย
    public int mana, maxMana;   // ค่ามานาใช้งานสกิล
    private boolean shieldActive = false;    
    public boolean onFire = false;
    public int fireTurns = 0;    
    public boolean poisoned = false;
    public int poisonTurns = 0;
    public boolean bleeding = false;
    public int bleedTurns = 0;
    
    public Entity(String name, int maxHp) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.baseDamage = 10;     // ตั้งค่าเริ่มต้น
        this.armor = 5;           // ตั้งค่าเริ่มต้น
        this.maxMana = 100;
        this.mana = 0;
        this.buffAttack = 0;
        this.poisoned    = false;
        this.poisonTurns = 0;
        this.bleeding    = false;
        this.bleedTurns  = 0;
        this.onFire      = false;
        this.fireTurns   = 0;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void setShield(boolean v) { shieldActive = v; }
    public boolean isShieldActive() { return shieldActive; }
}