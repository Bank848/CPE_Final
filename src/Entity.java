public class Entity {
    public String name;
    public int hp, maxHp;
    // สำหรับบัฟโจมตี
    public int buffAttack;
    // สำหรับ Fire Debuff
    public int baseDamage;      // พลังโจมตีพื้นฐานจากดาบ
    public int armor;           // ค่าพลังป้องกันลดความเสียหาย
    public int mana, maxMana;   // ค่ามานาใช้งานสกิล
    public boolean fireDebuff;
    private boolean shieldActive = false;

    public Entity(String name, int maxHp) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.baseDamage = 500;     // ตั้งค่าเริ่มต้น
        this.armor = 5;           // ตั้งค่าเริ่มต้น
        this.maxMana = 100;
        this.mana = this.maxMana;
        this.buffAttack = 0;
        this.fireDebuff = false;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void setShield(boolean v) { shieldActive = v; }
    public boolean isShieldActive() { return shieldActive; }

    // ล้างสถานะดีบัฟทั้งหมด (เรียกเมื่อซื้อ Necklace)
    public void removeDebuffs() {
        this.buffAttack = 0;
        // รีเซ็ตดีบัฟหรือบัฟอื่นๆ ที่เพิ่มในอนาคต
    }

    // ล้าง Fire Debuff (เรียกเมื่อซื้อ Holy Water)
    public void removeFireDebuff() {
        this.fireDebuff = false;
    }
}