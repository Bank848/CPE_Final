//BookwormUI.java
import java.awt.*;
import java.util.*; // ใช้ ArrayList, List, Map, HashMap, Queue, LinkedList
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

public class BookwormUI extends JFrame { // Main UI class for Bookworm Puzzle RPG
    private static final int GRID_SIZE = 8;
    private static final int MAX_LEVEL = 20;
    private static final boolean DEV_MODE = true;
    private static final int HP_INCREMENT = 4;
    private int gemMultiplier = 1; 
    private final Random rand = new Random();
    private float sfxVolume = 1.0f;

    private Grid gridModel;
    private FreeDictionary dict;
    private JButton[][] buttons;
    private List<Position> selectedPos;
    private List<JButton> selectedBtn;

    private Entity player;
    private Entity monster;
    private int currentLevel = 1;
    private int totaldmgPts = 0;
    private int totalGems = 99990;
    private boolean shieldActive = false;

    private JLabel wordLabel, gemLabel;
    private JLabel playerHpLabel, monsterHpLabel;
    private JLabel playerImgLabel, monsterImgLabel;
    private JLabel previewDamageLabel;
    private JLabel damageLabel;
    private JLabel armorLabel;
    private JLabel manaLabel;
    private JLabel kitsuneHpLabel;
    private JLabel kitsuneImgLabel;
    private JLabel monsterLevelLabel;

    private Set<Position> bannedLetters = new HashSet<>();
    private Map<String, ImageIcon> icons = new HashMap<>();
    protected JPanel gridPanel;
    private enum Reaction { HEAL, DEFEND, COUNTER }

    private Set<String> purchasedItems = new HashSet<>();
    private int holyWaterCount = 0;
    private boolean hasNecklace = false;    
    private boolean hasSake = false;    
    private boolean hasHolywater = false;    
    private Entity kitsune = null;
    private boolean kitsuneBuffActive = false;
    private JLabel kitsuneDmgLabel;
    private JLabel kitsuneManaLabel;
    private boolean kitsuneShieldActive = false;


    public BookwormUI() {
        super("Bookworm Puzzle RPG");
        // ————— เปิดให้ Swing วาด title bar เอง —————
        JDialog.setDefaultLookAndFeelDecorated(true);

        // ————— เซ็ตฟอนต์ให้ Title ของทุก Dialog —————
        Font dialogFont  = new Font("SansSerif", Font.BOLD, 24);
        Font titleFont   = dialogFont.deriveFont(28f);
        UIManager.put("OptionPane.messageFont", dialogFont);
        UIManager.put("OptionPane.buttonFont",  dialogFont);
        UIManager.put("OptionPane.font",        dialogFont);

        // สำคัญ: คีย์นี้จะไปเซ็ตฟอนต์ของ title bar
        UIManager.put("Dialog.titleFont",       titleFont);
        // สำรองไว้สำหรับบาง LookAndFeel
        UIManager.put("OptionPane.titleFont",   titleFont);
        loadIcons();
        gridModel = new Grid(GRID_SIZE);
        dict = new FreeDictionary();
        selectedPos = new ArrayList<>();
        selectedBtn = new ArrayList<>();
        player = new Entity("Hero", 100);
        monster = createMonsterForLevel(currentLevel);
        initUI();
        updateStatusLabels();
    }

    private void loadIcons() {
        String[] states = {"idle","attack","defend","heal"};
        // Hero
        for (String st: states) {
            icons.put("hero_" + st,
                new ImageIcon(getClass().getResource("/images/hero_" + st + ".png")));
        }
        // Monster & Boss
        for (int lvl = 1; lvl <= MAX_LEVEL; lvl++) {
            String keyBase;
            if (lvl % 5 == 0) {
                // บอสทุก 5 ด่าน
                switch(lvl) {
                    case 5:  keyBase = "boss5_highGoblin";   break;
                    case 10: keyBase = "boss10_kingGoblin";  break;
                    case 15: keyBase = "boss15_spiderQueen"; break;
                    case 20: keyBase = "boss20_dragonLord";  break;
                    default: keyBase = "boss" + lvl;         break;
                }
            } else if (lvl >= 11 && lvl <= 14) {
                keyBase = "enemy_spiderGirl";
            } else if (lvl >= 16 && lvl <= 19) {
                keyBase = "enemy_lizardGirl";
            } else {
                // ระหว่าง 1–4,6–9 ใช้ generic monster เดียวกัน
                keyBase = "enemy_basic";
            }
            for (String st: states) {
                icons.put(keyBase + "_" + st,
                    new ImageIcon(getClass().getResource("/images/" + keyBase + "_" + st + ".png")));
            }
        }
    }

    public void playSound(String soundFileName) {
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                getClass().getResource("/sounds/" + soundFileName))) {

            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);

            // adjust volume if supported
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                // convert linear 0.0–1.0 into decibels
                float dB = (float)(20 * Math.log10(sfxVolume <= 0.0f ? 0.0001f : sfxVolume));
                gain.setValue(dB);
            }

            clip.start();
        } catch (Exception e) {
            if (DEV_MODE) e.printStackTrace();
        }
    }

    private void openOptionsDialog() {
        // ฟอนต์ใหญ่สำหรับปุ่มและป้ายต่างๆ
        Font dialogFont  = new Font("SansSerif", Font.BOLD, 20);
        UIManager.put("OptionPane.messageFont", dialogFont);
        UIManager.put("OptionPane.buttonFont",  dialogFont);
    
        // สร้างสไลเดอร์ขนาดใหญ่
        JSlider slider = new JSlider(0, 100, (int)(sfxVolume * 100));
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setFont(dialogFont);                    // ขยายฟอนต์ของตัวเลขบนสไลเดอร์
        slider.setPreferredSize(new Dimension(500, 80)); // ขยายขนาดสไลเดอร์ให้กว้างและสูงขึ้น
        slider.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "SFX Volume",
            TitledBorder.CENTER,
            TitledBorder.TOP,
            dialogFont
        ));
    
        int result = JOptionPane.showConfirmDialog(
            this,
            slider,
            "Options",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            sfxVolume = slider.getValue() / 100f;
        }
    }

    private Entity createMonsterForLevel(int lvl) {
        boolean isBoss = (lvl % 5 == 0);
        int baseHp;
        
        if (isBoss) {
            Entity prev = createMonsterForLevel(lvl - 1);
            baseHp = prev.maxHp * 2;
        } else if (lvl > 1) {
            Entity prev = createMonsterForLevel(lvl - 1);
            baseHp = ((lvl - 1) % 5 == 0) ? prev.maxHp : prev.maxHp + HP_INCREMENT;
        } else {
            baseHp = 40 + lvl * 3;
        }
        Entity e = new Entity(
            isBoss ? "Boss Lv." + lvl : "Monster Lv." + lvl,
            baseHp
        );

        e.baseDamage = 5 + lvl * 2;

        return e;
    }

    private void applyLetterBanIfNeeded() {
        bannedLetters.clear();
        // แบนเฉพาะด่านบอส แต่ถ้ามี Necklace ให้ข้าม
        if (!hasNecklace && (currentLevel == 10 || currentLevel == 15 || currentLevel == 20)) {
            int total = GRID_SIZE * GRID_SIZE;
            int banCount = total * (20 + rand.nextInt(11)) / 100; // 20–30%
            while (bannedLetters.size() < banCount) {
                int r = rand.nextInt(GRID_SIZE), c = rand.nextInt(GRID_SIZE);
                bannedLetters.add(new Position(r, c));
            }
        }
    }

    private void initGridPanel(JPanel panel) {
        panel.removeAll();
        applyLetterBanIfNeeded();
    
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                Tile t = gridModel.getTile(r, c);
                JButton btn = new JButton();
    
                // สร้างข้อความตัวอักษร + คะแนน
                String html = String.format(
                    "<html><center>%c%s<br>"
                    + "<font color='black'>%d</font> / "
                    + "<font color='purple'>%d</font></center></html>",
                    t.letter,
                    t.isSpecial() ? "<font color='red'>*</font>" : "",
                    t.getDmgPts(),
                    t.getGemPts()
                );
                btn.setText(html);
                btn.setFont(new Font("Monospaced", Font.PLAIN, 20));
                btn.putClientProperty("pos", new Position(r, c));
    
                // ถ้าเป็นจุดพิเศษ ติดพื้นหลังชมพู
                if (t.isSpecial()) {
                    btn.setBackground(Color.PINK);
                }
    
                // ถ้าตำแหน่งนี้ถูกแบน ให้ปิดการใช้งาน และเปลี่ยนสีฟอนต์เป็นดำ
                Position pos = new Position(r, c);
                if (bannedLetters.contains(pos)) {
                    btn.setEnabled(false);
                    btn.setForeground(Color.BLACK);
                } else {
                    btn.setEnabled(true);
                }
    
                btn.addActionListener(e -> {
                    if (!btn.isEnabled()) return;  // บล็อกหากถูกแบน
                    onLetterClick((JButton) e.getSource());
                });
    
                buttons[r][c] = btn;
                panel.add(btn);
            }
        }
    
        panel.revalidate();
        panel.repaint();
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));

        Font defaultFont = new Font("SansSerif", Font.BOLD, 20);
        Font titleFont = defaultFont.deriveFont(24f);

        // === TOP BAR ===
        JPanel top = new JPanel(new GridLayout(1,4,10,10));
        top.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // 1) HERO PANEL
        JPanel heroPanel = new JPanel(new BorderLayout(5,5));
        TitledBorder heroBorder = BorderFactory.createTitledBorder("Hero");
        heroBorder.setTitleFont(titleFont);
        heroPanel.setBorder(heroBorder);
        playerImgLabel = new JLabel(icons.get("hero_idle"));
        heroPanel.add(playerImgLabel, BorderLayout.WEST);
        JPanel heroStats = new JPanel(new GridLayout(4,1));
        playerHpLabel = new JLabel(); playerHpLabel.setFont(defaultFont);
        manaLabel = new JLabel(); manaLabel.setFont(defaultFont);
        damageLabel = new JLabel(); damageLabel.setFont(defaultFont);
        armorLabel = new JLabel(); armorLabel.setFont(defaultFont);
        heroStats.add(playerHpLabel); heroStats.add(manaLabel);
        heroStats.add(damageLabel); heroStats.add(armorLabel);
        heroPanel.add(heroStats, BorderLayout.CENTER);
        top.add(heroPanel);

        // 2) KITSUNE PANEL
        JPanel kitsunePanel = new JPanel(new BorderLayout(5,5));
        TitledBorder kitsuneBorder = BorderFactory.createTitledBorder("Ally");
        kitsuneBorder.setTitleFont(titleFont);
        kitsunePanel.setBorder(kitsuneBorder);
        kitsuneImgLabel = new JLabel(); kitsunePanel.add(kitsuneImgLabel, BorderLayout.WEST);
        JPanel kitsuneStats = new JPanel(new GridLayout(3,1));
        kitsuneHpLabel = new JLabel(); kitsuneHpLabel.setFont(defaultFont);
        kitsuneManaLabel = new JLabel(); kitsuneManaLabel.setFont(defaultFont);
        kitsuneDmgLabel = new JLabel(); kitsuneDmgLabel.setFont(defaultFont);
        kitsuneStats.add(kitsuneHpLabel); kitsuneStats.add(kitsuneManaLabel); kitsuneStats.add(kitsuneDmgLabel);
        kitsunePanel.add(kitsuneStats, BorderLayout.CENTER);
        top.add(kitsunePanel);

        // 3) SELECTED WORD PANEL
        JPanel selectedPanel = new JPanel(new BorderLayout(5,5));
        TitledBorder selectedBorder = BorderFactory.createTitledBorder("Selected Word");
        selectedBorder.setTitleFont(titleFont);
        selectedPanel.setBorder(selectedBorder);
        wordLabel = new JLabel("Current: "); wordLabel.setFont(defaultFont);
        previewDamageLabel = new JLabel("DMG Preview: 0"); previewDamageLabel.setFont(defaultFont);
        selectedPanel.add(wordLabel, BorderLayout.NORTH);
        selectedPanel.add(previewDamageLabel, BorderLayout.SOUTH);
        top.add(selectedPanel);

        // 4) MONSTER PANEL
        JPanel monsterPanel = new JPanel(new BorderLayout(5,5));
        TitledBorder monsterBorder = BorderFactory.createTitledBorder("Enemy");
        monsterBorder.setTitleFont(titleFont);
        monsterPanel.setBorder(monsterBorder);
        monsterImgLabel = new JLabel(getMonsterIcon("idle")); monsterPanel.add(monsterImgLabel, BorderLayout.WEST);
        JPanel monsterStats = new JPanel(new GridLayout(3,1));
        monsterHpLabel = new JLabel(); monsterHpLabel.setFont(defaultFont);
        monsterLevelLabel = new JLabel("Level: " + currentLevel); monsterLevelLabel.setFont(defaultFont);
        monsterStats.add(monsterHpLabel); monsterStats.add(monsterLevelLabel);
        monsterPanel.add(monsterStats, BorderLayout.CENTER);
        top.add(monsterPanel);

        add(top, BorderLayout.NORTH);

        // === GRID PANEL ===
        gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 4, 4));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        initGridPanel(gridPanel); // ใน initGridPanel ให้แก้ Font Monospaced,16 -> Monospaced,24
        add(gridPanel, BorderLayout.CENTER);

        // === CONTROLS ===
        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
        TitledBorder controlBorder = BorderFactory.createTitledBorder("Controls");
        controlBorder.setTitleFont(titleFont);
        control.setBorder(controlBorder);
        gemLabel = new JLabel("Gems: " + totalGems); gemLabel.setFont(defaultFont);
        JButton submitBtn = new JButton("Submit"); submitBtn.setFont(defaultFont); submitBtn.setPreferredSize(new Dimension(150,50));
        JButton clearBtn = new JButton("Clear"); clearBtn.setFont(defaultFont); clearBtn.setPreferredSize(new Dimension(150,50));
        JButton shopBtn = new JButton("Shop"); shopBtn.setFont(defaultFont); shopBtn.setPreferredSize(new Dimension(150,50));
        JButton optionsBtn = new JButton("Options"); optionsBtn.setFont(defaultFont); optionsBtn.setPreferredSize(new Dimension(150,50));
        submitBtn.addActionListener(e -> onSubmit(gridPanel));
        clearBtn.addActionListener(e -> clearSelection());
        shopBtn.addActionListener(e -> openShop());
        optionsBtn.addActionListener(e -> openOptionsDialog());
        control.add(submitBtn); control.add(clearBtn); control.add(shopBtn); control.add(optionsBtn); control.add(gemLabel);
        if (DEV_MODE) {
            JButton skipBtn = new JButton("Skip Level"); skipBtn.setFont(defaultFont); skipBtn.setPreferredSize(new Dimension(150,50));
            skipBtn.addActionListener(e -> nextLevel(gridPanel)); control.add(skipBtn);
        }
        add(control, BorderLayout.SOUTH);

        // ปรับให้เต็มหน้าจอโดยไม่ overflow
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    public JPanel getGridPanel() {
        return gridPanel;
    }

    public int getGems() {
        return totalGems;
    }

    // Overload เพื่อรองรับ 2-player
    public void performPlayerAction(JPanel grid, Entity actor) {
        // สลับ player ภายในเพื่องานเดิม
        Entity prevPlayer = player;
        player = actor;
        performPlayerAction(grid);
        player = prevPlayer;
    }

    private ImageIcon getMonsterIcon(String state) {
        String keyBase;
        if (currentLevel % 5 == 0) {
            // บอสด่าน 5,10,15,20
            switch (currentLevel) {
                case 5:  keyBase = "boss5_highGoblin";   break;
                case 10: keyBase = "boss10_kingGoblin";  break;
                case 15: keyBase = "boss15_spiderQueen"; break;
                case 20: keyBase = "boss20_dragonLord";  break;  // <-- ให้ตรงกับ loadIcons()
                default: keyBase = "boss" + currentLevel;         break;
            }
        } else if (currentLevel >= 11 && currentLevel <= 14) {
            keyBase = "enemy_spiderGirl";
        } else if (currentLevel >= 16 && currentLevel <= 19) {
            keyBase = "enemy_lizardGirl";
        } else {
            // ด่าน 1–4 และ 6–9
            keyBase = "enemy_basic";
        }
    
        // ถ้าไม่มี ก็ fallback ให้ดูอย่างน้อยเจอ generic
        return icons.getOrDefault(
            keyBase + "_" + state,
            icons.get("enemy_basic_idle")
        );
    }

    private void updateStatusLabels() {
        // Hero
        playerHpLabel.setText( String.format("HP: %d / %d", player.hp, player.maxHp) );
        damageLabel.setText( String.format("ATK: %d", player.baseDamage) );
        manaLabel.setText(   String.format("Mana: %d / %d", player.mana, player.maxMana) );
        armorLabel.setText(  String.format("Armor: %d", player.armor) );
    
        // Kitsune (only if joined)
        if (kitsune != null) {
            kitsuneHpLabel.setText( String.format("HP: %d / %d", kitsune.hp, kitsune.maxHp) );
            kitsuneDmgLabel.setText( String.format("ATK: %d", kitsune.baseDamage) );
            kitsuneManaLabel.setText( String.format("Mana: %d / %d", kitsune.mana, kitsune.maxMana) );
            kitsuneImgLabel.setIcon(new ImageIcon(
                getClass().getResource("/images/kitsune_idle.png")
            ));
        } else {
            kitsuneHpLabel.setText("");
            kitsuneDmgLabel.setText("");
            kitsuneManaLabel.setText("");
            kitsuneImgLabel.setIcon(null);
        }
    
        // Monster
        monsterHpLabel.setText( String.format("HP: %d / %d", monster.hp, monster.maxHp) );

        // Level
        if (monsterLevelLabel != null) {
            monsterLevelLabel.setText("Level: " + currentLevel);
        }    

        // Gems
        gemLabel.setText("Gems: " + totalGems);
    }

    private void onLetterClick(JButton btn) {
        playSound("select.wav");
        Position p = (Position)btn.getClientProperty("pos");
        if (selectedPos.contains(p)) {
            int idx = selectedPos.indexOf(p);
            if (idx == selectedPos.size()-1) {
                JButton b = selectedBtn.remove(idx);
                Position rm = selectedPos.remove(idx);
                b.setBackground(gridModel.getTile(rm.row,rm.col).isSpecial() ? Color.PINK : null);
                updateWordLabel();
            }
            return;
        }
        if (!selectedPos.isEmpty()) {
            Position last = selectedPos.get(selectedPos.size()-1);
            if (Math.abs(p.row-last.row)>1 || Math.abs(p.col-last.col)>1) return;
        }
        selectedPos.add(p);
        selectedBtn.add(btn);
        btn.setBackground(Color.YELLOW);
        updateWordLabel();
    }


    private void updateWordLabel() {
        StringBuilder sb = new StringBuilder();
        int tileDmg = 0;
        boolean special = false;
        for (Position p : selectedPos) {
            Tile t = gridModel.getTile(p.row,p.col);
            sb.append(t.letter);
            tileDmg += t.getDmgPts();
            if (t.isSpecial()) special = true;
        }
        String word = sb.toString().toLowerCase();
        wordLabel.setText("Current: " + word);
        previewDamageLabel.setText("DMG Preview: " +
        (player.baseDamage
         + player.buffAttack
         + (special ? tileDmg * 2 : tileDmg)
        )
    );    
    }


    public void onSubmit(JPanel grid) {
        playSound("attack.wav");
        playerImgLabel.setIcon(icons.get("hero_attack"));
        SwingUtilities.invokeLater(() -> {
            try { Thread.sleep(300); } catch (Exception ignored) {}
            performPlayerAction(grid);
        });
    }

    // Decide reaction for monster's immediate response
    private Reaction decideReaction() {
        boolean isBoss = (currentLevel % 5 == 0);
    
        // โอกาส HEAL เหมือนเดิม
        double healChance = isBoss ? 0.25 : 0.10;
        // โอกาส DEFEND (รวม Counter) เพิ่มหลังด่าน 15
        double defendChance = isBoss ? 0.50 : 0.25;
        if (!isBoss && currentLevel > 15) {
            defendChance += 0.20;  // +20%
        }
        // ถ้ามีบัฟ Kitsune ก็ล็อก defendChance = 50%
        if (kitsuneBuffActive && !isBoss && currentLevel > 15) {
            defendChance = 0.50;
        }
    
        double roll = rand.nextDouble();
        if (roll < healChance) return Reaction.HEAL;
        else if (roll < defendChance) return Reaction.DEFEND;
        else return Reaction.COUNTER;
    }

    private void performPlayerAction(JPanel grid) {
        StringBuilder sb = new StringBuilder();
        boolean special = false;
        for (Position p: selectedPos) {
            Tile t = gridModel.getTile(p.row,p.col);
            sb.append(t.letter);
            if (t.isSpecial()) special = true;
        }
        String word = sb.toString().toLowerCase();
        if (word.length()<3 || !dict.contains(word)) {
            JOptionPane.showMessageDialog(this,"Word invalid.");
            playerImgLabel.setIcon(icons.get("hero_idle"));
        } else {
            // แยกคำนวณ damage จาก tile
            int tileDmg = 0, gainG = 0;
            for (Position p: selectedPos) {
                Tile t = gridModel.getTile(p.row,p.col);
                tileDmg += t.getDmgPts();
                gainG += t.getGemPts();
            }
            // ถ้าเป็น special ให้คูณสองเฉพาะ tile damage
            if (special) {
                tileDmg *= 2;
                JOptionPane.showMessageDialog(this,"Special tile! Damage doubled and board will reset.");
            }
            // สร้าง raw damage โดยบวกบัฟหลังคูณสอง
            int raw = player.baseDamage + tileDmg + player.buffAttack;            // รีเซ็ตบัฟใช้ครั้งเดียว
            player.buffAttack = 0;

            // เพิ่มสถิติ
            totaldmgPts += tileDmg;
            totalGems += gainG * gemMultiplier;

            // สถานะ monster ตอบโต้
            Reaction react = decideReaction();
            // ให้เกราะ monster ถ้า reaction เป็น DEFEND
            if (react == Reaction.DEFEND) {
                monster.setShield(true);
                playSound((currentLevel%5==0)?"boss_defend.wav":"enemy_defend.wav");
                monsterImgLabel.setIcon(getMonsterIcon("defend"));
                new Timer(1000,e->{ monsterImgLabel.setIcon(getMonsterIcon("idle")); ((Timer)e.getSource()).stop(); }).start();
            }
            // ถ้ามี shield ลดครึ่ง
            if (monster.isShieldActive()) {
                raw /= 2;
                monster.setShield(false);
                JOptionPane.showMessageDialog(this, monster.name+" blocks 50% of your damage!");
            }
            // ทำ damage ให้ monster
            monster.hp -= raw;
            JOptionPane.showMessageDialog(this,String.format("You dealt %d dmg, +%d Gems", raw, gainG));
            new Timer(1000,e->{ playerImgLabel.setIcon(icons.get("hero_idle")); ((Timer)e.getSource()).stop(); }).start();

            // อัพเดตกริด
            if (special) gridModel = new Grid(GRID_SIZE);
            else gridModel.removeAndCollapse(selectedPos);
            initGridPanel(grid);
            updateStatusLabels();

            // ถ้า monster ยังไม่ตาย ให้มันตอบโต้
            if (monster.hp > 0) {
                reactToPlayerAttack(react);
            } else {
                if ((currentLevel == 11 || currentLevel == 16) && hasSake && hasHolywater) {
                    // เกิดอีเวนต์ Kitsune
                    openKitsuneEvent(grid);
                } else if (currentLevel % 5 == 0) {
                    boolean spawn = shouldSpawnMerchant();
                    handleAfterBoss(spawn, grid);
                } else {
                    nextLevel(grid);
                }
            }
    }
        applyDebuffTicks();
        clearSelection();
        updateStatusLabels();
    }

    private void reactToPlayerAttack(Reaction react) {

        boolean isBoss = (currentLevel%5==0);
        switch(react) {
            case HEAL:
                int amt = isBoss?30:15;
                monster.hp = Math.min(monster.maxHp, monster.hp + amt);
                playSound(isBoss?"boss_heal.wav":"enemy_heal.wav");
                monsterImgLabel.setIcon(getMonsterIcon("heal"));
                new Timer(1000,e->{ monsterImgLabel.setIcon(getMonsterIcon("idle")); ((Timer)e.getSource()).stop(); }).start();
                JOptionPane.showMessageDialog(this, monster.name+" heals for "+amt+" HP!");
                break;
            case DEFEND:
            case COUNTER:
                if (kitsune != null && react == Reaction.COUNTER && rand.nextDouble() < 0.3) {
                    monster.hp -= kitsune.baseDamage;
                    JOptionPane.showMessageDialog(this, "Kitsune COUNTER Attack " + kitsune.baseDamage + " Damage!");
                }            
                int rawDmg = monster.baseDamage + rand.nextInt(5); //สุ่ม 0–4 ดาเมจเพิ่มมาด้วย
                int dmg = Math.max(1, rawDmg - player.armor);
                if (shieldActive) { dmg/=2; shieldActive=false; JOptionPane.showMessageDialog(this,"Your shield blocks 50% of the counterattack!"); }
                player.hp -= dmg;
                if (kitsune != null) {
                    int kdmg = dmg;
                    if (kitsuneShieldActive) {
                        kdmg /= 2;
                        kitsuneShieldActive = false;
                        JOptionPane.showMessageDialog(this, "Kitsune's shield blocks 50% of the attack!");
                    }
                    kitsune.hp -= kdmg;
                    if (kitsune.hp < 0) kitsune.hp = 0;
                }                
                playSound(isBoss?"boss_attack.wav":"enemy_attack.wav");
                monsterImgLabel.setIcon(getMonsterIcon("attack"));
                new Timer(1000,e->{ monsterImgLabel.setIcon(getMonsterIcon("idle")); ((Timer)e.getSource()).stop(); }).start();
                JOptionPane.showMessageDialog(this, monster.name+" counterattacks for "+dmg+" damage!");
                // 1) Poison 30% ด่าน 11–15
                if (currentLevel >= 11 && currentLevel <= 15 && rand.nextDouble() < 0.30) {
                    // ถ้ามี Kitsune อยู่ จะสุ่ม target 0=hero,1=kitsune,2=both
                    int target = (kitsune != null) ? rand.nextInt(3) : 0;

                    if (target == 0 || target == 2) {
                        player.poisoned    = true;
                        player.poisonTurns = 2;
                        JOptionPane.showMessageDialog(this, "You have been poisoned! (2 turns)");
                    }
                    if (kitsune != null && (target == 1 || target == 2)) {
                        kitsune.poisoned    = true;
                        kitsune.poisonTurns = 2;
                        JOptionPane.showMessageDialog(this, "Your Kitsune has been poisoned! (2 turns)");
                    }
                }

                // 2) Bleeding 30% ด่าน 16–19
                if (currentLevel >= 16 && currentLevel <= 19 && rand.nextDouble() < 0.30) {
                    int target = (kitsune != null) ? rand.nextInt(3) : 0;

                    if (target == 0 || target == 2) {
                        player.bleeding   = true;
                        player.bleedTurns = 2;
                        JOptionPane.showMessageDialog(this, "You are bleeding! (2 turns)");
                    }
                    if (kitsune != null && (target == 1 || target == 2)) {
                        kitsune.bleeding   = true;
                        kitsune.bleedTurns = 2;
                        JOptionPane.showMessageDialog(this, "Your Kitsune is bleeding! (2 turns)");
                    }
                }

                // 3) Fire 30% บอสด่าน 20
                if (currentLevel == 20 && rand.nextDouble() < 0.30) {
                    int target = (kitsune != null) ? rand.nextInt(3) : 0;

                    if (target == 0 || target == 2) {
                        player.onFire    = true;
                        player.fireTurns = Integer.MAX_VALUE;
                        JOptionPane.showMessageDialog(this, "The dragon sets YOU ablaze!");
                    }
                    if (kitsune != null && (target == 1 || target == 2)) {
                        kitsune.onFire    = true;
                        kitsune.fireTurns = Integer.MAX_VALUE;
                        JOptionPane.showMessageDialog(this, "The dragon sets your KITSUNE ablaze!");
                    }
                }
                if (player.hp<=0) { playSound("gameover.wav"); JOptionPane.showMessageDialog(this,"You have been defeated..."); System.exit(0);}                
                break;
        }
        updateStatusLabels();
    }

    public void clearSelection() {
        // ล้างการเลือก และคืนพื้นหลังตามสกิลพิเศษ
        for (JButton b : selectedBtn) {
            Position p = (Position)b.getClientProperty("pos");
            if (gridModel.getTile(p.row, p.col).isSpecial()) {
                b.setBackground(Color.PINK);
            } else {
                b.setBackground(null);
            }
        }
        selectedBtn.clear();
        selectedPos.clear();
        wordLabel.setText("Current: ");
    }

    private void refreshGrid() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                Tile t = gridModel.getTile(r, c);
                // สร้าง HTML เหมือน initGridPanel
                String html = String.format(
                    "<html><center>%c%s<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                    t.letter,
                    t.isSpecial() ? "<font color='red'>*</font>" : "",
                    t.getDmgPts(),
                    t.getGemPts()
                );
                buttons[r][c].setText(html);
                // เปลี่ยน background ถ้าเป็น special
                if (t.isSpecial()) {
                    buttons[r][c].setBackground(Color.PINK);
                } else {
                    buttons[r][c].setBackground(null);
                }
            }
        }
    }

    private void nextLevel(JPanel gridPanel) {
            currentLevel++;
            if (currentLevel > MAX_LEVEL) {
                JOptionPane.showMessageDialog(this, "You WIN!");
                System.exit(0);
            }
            monster = createMonsterForLevel(currentLevel);
            monsterImgLabel.setIcon(getMonsterIcon("idle"));
            // แจ้งเตือนเลเวลอัพ
            JOptionPane.showMessageDialog(this, "Level up! Now facing " + monster.name);
            // เมื่อถึงด่าน 11 หรือ 16 ให้บอกว่ามีของใหม่ในร้านค้า
            if (currentLevel == 11) {
                JOptionPane.showMessageDialog(this,
                    "ํYou can buy new items in the shop!"
                );
            } else if (currentLevel == 16) {
                JOptionPane.showMessageDialog(this,
                    "ํYou can buy new items in the shop!"
                );
            }
            // รีเซ็ตกริดและสถานะ
            gridModel = new Grid(GRID_SIZE);
            initGridPanel(gridPanel);
            clearSelection();
            updateStatusLabels();
        }

    private void openShop() {
        openShopForPlayer(player);
    }
    
    public void openShopForPlayer(Entity e) {
        // เตรียมรายการสินค้า dynamic ตามด่าน
        List<String> opts = new ArrayList<>(List.of(
            "Heal (5 Gems)",
            "Shield (3 Gems)",
            "BuffAtk (4 Gems)",
            "Shuffle (6 Gems)"
        ));
        if (currentLevel >= 11) {
            opts.add("Antidote (8 Gems)");
        }
        if (currentLevel >= 16) {
            opts.add("Bandage (10 Gems)");
        }
        opts.add("Close");
    
        String[] options = opts.toArray(new String[0]);
        while (true) {
            int choice = JOptionPane.showOptionDialog(
                this,
                "Gems: " + totalGems + "\nSelect Item to buy",
                "Shop",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
            );
            // กดปิดหรือเลือก Close --> ออก
            if (choice < 0 || options[choice].equals("Close")) {
                break;
            }
    
            String opt = options[choice];
            int cost = Integer.parseInt(opt.replaceAll(".*\\((\\d+) Gems\\)", "$1"));
            if (totalGems < cost) {
                JOptionPane.showMessageDialog(this, "Not enough Gems");
                continue;
            }
            totalGems -= cost;
    
            switch (opt) {
                case "Heal (5 Gems)":
                    e.hp = Math.min(e.maxHp, e.hp + 20);
                    JOptionPane.showMessageDialog(this, "Healed 20 HP!");
                    break;
    
                case "Shield (3 Gems)":
                    shieldActive = true;
                    if (kitsune != null) kitsuneShieldActive = true;
                    JOptionPane.showMessageDialog(this, "Shield activated for 1 turn!");
                    break;
    
                case "BuffAtk (4 Gems)":
                    e.buffAttack += 10;
                    JOptionPane.showMessageDialog(this, "Attack buff +10 for this round!");
                    break;
    
                case "Shuffle (6 Gems)":
                    gridModel.shuffle();
                    refreshGrid();
                    clearSelection();
                    JOptionPane.showMessageDialog(this, "Board shuffled!");
                    break;
    
                case "Antidote (8 Gems)":
                    e.poisoned    = false;
                    e.poisonTurns = 0;
                    JOptionPane.showMessageDialog(this, "Antidote used! Poison removed.");
                    break;
    
                case "Bandage (10 Gems)":
                    e.bleeding   = false;
                    e.bleedTurns = 0;
                    e.hp = Math.min(e.maxHp, e.hp + 10);
                    JOptionPane.showMessageDialog(this, "Bandage used! Bleeding stopped and +10 HP.");
                    break;
            }
    
            updateStatusLabels();
        }
    }

    private void handleAfterBoss(boolean spawnMerchant, JPanel grid) {
        // 1. ถามว่าจะเปิดกล่องรางวัลหรือไม่
        int open = JOptionPane.showConfirmDialog(
            this,
            "You found Loot Box Do you want to open it?",
            "Loot Box",
            JOptionPane.YES_NO_OPTION
        );
        if (open == JOptionPane.YES_OPTION) {
            openLootBox();
        }
    
        // 2. ตรวจสอบโอกาสเกิดพ่อค้าลับ
        if (shouldSpawnMerchant()) {
            openSecretMerchant();
        }
    }

    private boolean shouldSpawnMerchant() {
        // ด่าน 10 กับ 15 เกิดแน่นอน
        if (currentLevel == 10 || currentLevel == 15) {
            return true;
        }
        // ด่านอื่น ๆ ให้โอกาสเกิด 50%
        return rand.nextDouble() < 0.50;
    }

    private void openSecretMerchant() {
        // ซ่อนกริด
        remove(gridPanel);
        // เตรียม merchant panel
        JPanel merchantPanel = new JPanel(new BorderLayout(10,10));
        Font btnFont = new Font("SansSerif", Font.BOLD, 24);
        // รายการสินค้า
        String[] options = {
            "Weapon of Hero (100 Gems)",
            "Armor of Hero (100 Gems)",
            "Rainbow Potion (50 Gems)",
            "Red Potion (80 Gems)",
            "Necklace of Hero (80 Gems)",
            "Holy Water (120 Gems)",
            "Sake (30 Gems)",
            "Quit Shop",
        };
        JPanel buttonPanel = new JPanel(new GridLayout(options.length,1,5,5));
        // แสดงปุ่มแต่ละสินค้า
        for (String opt : options) {
            JButton b = new JButton(opt);
            b.setFont(btnFont); 
            // ถ้าเคยซื้อแล้ว ยกเลิกการสร้างปุ่มสำหรับรายการถาวร
            if (purchasedItems.contains(opt) && !opt.startsWith("Holy Water")) continue;
            b.addActionListener(e -> {
                if (opt.equals("Quit Shop")) {
                    // คืนกริดเดิม และไปด่านถัดไป
                    remove(merchantPanel);
                    add(gridPanel, BorderLayout.CENTER);
                    nextLevel(gridPanel);
                    validate(); repaint();
                    return;
                }
                int cost = Integer.parseInt(
                    opt.replaceAll(".*\\((\\d+) Gems\\)", "$1")
                );                if (totalGems < cost) {
                    JOptionPane.showMessageDialog(this, "Not enough gems");
                    return;
                }
                totalGems -= cost;
                // จัดการซื้อ
                switch(opt) {
                    case "Weapon of Hero (100 Gems)":
                        player.baseDamage += 64;
                        JOptionPane.showMessageDialog(this, "Buy Weapon of Hero: +64 ATK");
                        break;
                    case "Armor of Hero (100 Gems)":
                        player.armor += 20;
                        JOptionPane.showMessageDialog(this, "Buy Armor of Hero: +20 Armor");
                        break;
                    case "Rainbow Potion (50 Gems)":
                        gemMultiplier = 2;
                        JOptionPane.showMessageDialog(this,"Rainbow Potion: Gain Gems x2");
                        break;
                    case "Red Potion (80 Gems)":
                        player.maxHp += 100; player.hp += 100;
                        player.maxMana += 50; player.mana += 50;
                        JOptionPane.showMessageDialog(this, "Red Potion: MaxHP +100, Max Mana +50, Hp +100, Mana +50");
                        break;
                    case "Necklace of Hero (80 Gems)":
                        hasNecklace = true;
                        JOptionPane.showMessageDialog(this, "Remove ban-letter effect!");
                        break;
                    case "Holy Water (120 Gems)":
                        hasHolywater = true;
                        holyWaterCount++;
                        JOptionPane.showMessageDialog(
                            this,
                            String.format("Bought Holy Water! You now have %d bottle(s).", holyWaterCount)
                        );
                        break;
                    case "Sake (30 Gems)":
                        hasSake = true;
                        JOptionPane.showMessageDialog(this, "Sake?");
                        break;
                }
                updateStatusLabels();
                // ถ้าเป็นสินค้าถาวร (ไม่ใช่ Holy Water) ให้ลบปุ่มทิ้ง
                if (!opt.equals("Holy Water (120 Gems)")) {
                    buttonPanel.remove(b);
                    purchasedItems.add(opt);
                    buttonPanel.revalidate(); buttonPanel.repaint();
                }
            });
            buttonPanel.add(b);
        }
        merchantPanel.add(buttonPanel, BorderLayout.CENTER);
        // 2. ขวา: รูปพ่อค้า
        ImageIcon merchantIcon = new ImageIcon(getClass().getResource("/images/secret_merchant.png"));
        JLabel merchantImg = new JLabel(merchantIcon);
        merchantPanel.add(merchantImg, BorderLayout.EAST);
        // แสดง
        add(merchantPanel, BorderLayout.CENTER);
        validate(); repaint();
    }

    private void openLootBox() {
        int roll = rand.nextInt(100);
    
        if (roll < 20) { // 20% ดาบเพิ่ม Base Damage +20% ถึง +40%
            int percent = 20 + rand.nextInt(21);
            player.baseDamage = (int)(player.baseDamage * (1 + percent / 100.0));
            JOptionPane.showMessageDialog(this, "Sword Buff: Base ATK +" + percent + "%");
    
        } else if (roll < 35) { // 15% ดีบัฟลด Base Damage –20% ถึง –30%
            int percent = 20 + rand.nextInt(11);
            player.baseDamage = (int)(player.baseDamage * (1 - percent / 100.0));
            JOptionPane.showMessageDialog(this, "Debuff: Base ATK –" + percent + "%");
    
        } else if (roll < 50) { // 15% Max HP +50
            player.maxHp += 50;
            player.hp = Math.min(player.hp + 50, player.maxHp);
            JOptionPane.showMessageDialog(this, "Max HP +50");
    
        } else if (roll < 60) { // 10% Max HP -10
            player.maxHp = Math.max(1, player.maxHp - 10);
            player.hp = Math.min(player.hp, player.maxHp);
            JOptionPane.showMessageDialog(this, "Max HP -10");
    
        } else if (roll < 70) { // 10% Armor +5 ถึง +10
            int amount = 5 + rand.nextInt(6);
            player.armor += amount;
            JOptionPane.showMessageDialog(this, "Armor +" + amount);
    
        } else if (roll < 80) { // 10% Armor -5 ถึง -10
            int amount = 5 + rand.nextInt(6);
            player.armor = Math.max(0, player.armor - amount);
            JOptionPane.showMessageDialog(this, "Armor -" + amount);
    
        } else if (roll < 90) { // 10% Max Mana +20% ถึง +40%
            int percent = 20 + rand.nextInt(21);
            player.maxMana = (int)(player.maxMana * (1 + percent / 100.0));
            player.mana = player.maxMana;
            JOptionPane.showMessageDialog(this, "Max Mana +" + percent + "%");
    
        } else { // 10% Max Mana -20% ถึง -40%
            int percent = 20 + rand.nextInt(21);
            player.maxMana = (int)(player.maxMana * (1 - percent / 100.0));
            player.mana = Math.min(player.mana, player.maxMana);
            JOptionPane.showMessageDialog(this, "Max Mana –" + percent + "%");
        }
    
        updateStatusLabels();
    }

    private void openKitsuneEvent(JPanel grid) {
        remove(gridPanel);
        JPanel eventPanel = new JPanel(new BorderLayout(10,10));
        Font btnFont = new Font("SansSerif", Font.BOLD, 24);

        // ปุ่มเลือก
        String[] opts = {"Help Kitsune", "Not Help"};
        JPanel btns = new JPanel(new GridLayout(opts.length,1,5,5));
        for(String opt: opts) {                          
            JButton b = new JButton(opt);
            b.setFont(btnFont);  
            b.addActionListener(e -> {
                if (opt.equals("Help Kitsune")) {
                    if (hasSake && hasHolywater) {
                        hasSake = false;
                        holyWaterCount -= 1;
                        kitsune = new Entity("Kitsune", 200);
                        kitsune.baseDamage = 30;
                        kitsune.armor = 10;
                        kitsune.maxMana = 100;
                        kitsune.mana = 100;
                        kitsuneBuffActive = true;  // จะใช้ในด่าน 20
                        JOptionPane.showMessageDialog(this, "Kitsune Join your team!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Kitsune: need Sake and Holy Water for healing!");
                    }
                }
                // กลับสู่หน้าบอร์ด
                remove(eventPanel);
                add(gridPanel, BorderLayout.CENTER);
                nextLevel(grid);
                validate(); 
                repaint();
            });
            btns.add(b);
        }
        eventPanel.add(btns, BorderLayout.CENTER);
    
        // รูป Kitsune ด้านขวา
        ImageIcon ik = new ImageIcon(getClass().getResource("/images/kitsune.png"));
        eventPanel.add(new JLabel(ik), BorderLayout.EAST);
    
        add(eventPanel, BorderLayout.CENTER);
        validate(); repaint();
    }
    private void applyDebuffTicks() {
        // --- POISON (Hero + Kitsune) ---
        if (player.poisoned && player.poisonTurns > 0) {
            player.hp -= 5;
            player.poisonTurns--;
            if (player.poisonTurns == 0) player.poisoned = false;
            JOptionPane.showMessageDialog(this,
                "You suffer 5 poison damage! (" + player.poisonTurns + " turns left)");
        }
        if (kitsune != null && kitsune.poisoned && kitsune.poisonTurns > 0) {
            kitsune.hp -= 5;
            kitsune.poisonTurns--;
            if (kitsune.poisonTurns == 0) kitsune.poisoned = false;
            JOptionPane.showMessageDialog(this,
                "Kitsune suffers 5 poison damage! (" + kitsune.poisonTurns + " turns left)");
        }
    
        // --- BLEEDING (Hero + Kitsune) ---
        if (player.bleeding && player.bleedTurns > 0) {
            player.hp -= 7;
            player.bleedTurns--;
            if (player.bleedTurns == 0) player.bleeding = false;
            JOptionPane.showMessageDialog(this,
                "You lose 7 HP from bleeding! (" + player.bleedTurns + " turns left)");
        }
        if (kitsune != null && kitsune.bleeding && kitsune.bleedTurns > 0) {
            kitsune.hp -= 7;
            kitsune.bleedTurns--;
            if (kitsune.bleedTurns == 0) kitsune.bleeding = false;
            JOptionPane.showMessageDialog(this,
                "Kitsune loses 7 HP from bleeding! (" + kitsune.bleedTurns + " turns left)");
        }
    
        // --- FIRE (Hero) ---
        if (player.onFire) {
            // if we still have a delay and HW to spend, ask…
            if (player.fireTurns > 0 && holyWaterCount > 0) {
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "You are burning! Use Holy Water to extinguish for 5 turns?",
                    "Holy Water",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    player.fireTurns = 5;
                    holyWaterCount--;
                    JOptionPane.showMessageDialog(
                        this,
                        String.format("You used Holy Water. Remaining: %d bottle(s).", holyWaterCount)
                    );
                }
            } else {
                // take the burn damage
                player.hp -= 9;
                JOptionPane.showMessageDialog(this, "You burn for 9 fire damage!");
                if (player.fireTurns > 0) player.fireTurns--;
            }
            if (player.fireTurns == 0) {
                player.onFire = false;
            }
        }
    
        // --- FIRE (Kitsune) ---
        if (kitsune != null && kitsune.onFire) {
            if (kitsune.fireTurns > 0) {
                kitsune.fireTurns--;
                kitsune.hp -= 9;
                JOptionPane.showMessageDialog(this, "Kitsune burns for 9 fire damage!");
            }
            if (kitsune.fireTurns == 0) {
                kitsune.onFire = false;
            }
        }
    
        updateStatusLabels();
    }

}
