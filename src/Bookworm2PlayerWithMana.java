import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class Bookworm2PlayerWithMana extends JFrame {
    private static final int GRID_SIZE = 8;
    private static final boolean DEV_MODE = true;

    private Grid gridModel;
    private FreeDictionary dict;
    private JButton[][] buttons;
    private List<Position> selectedPos;
    private List<JButton> selectedBtn;

    private Entity player1, player2;
    private Entity currentPlayer, opponent;

    private int p1Gems, p2Gems;
    private int p1Wins = 0, p2Wins = 0;
    private int currentRound = 1;

    private JLabel wordLabel, dmgPreviewLabel;
    private JLabel scoreLabel, roundLabel;
    private JLabel p1HpLabel, p1ManaLabel, p1GemsLabel, p1ImgLabel;
    private JLabel p2HpLabel, p2ManaLabel, p2GemsLabel, p2ImgLabel;

    private Map<String, ImageIcon> icons = new HashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Bookworm2PlayerWithMana());
    }

    public Bookworm2PlayerWithMana() {
        super("Bookworm 2 Player Mode");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        dict = new FreeDictionary();
        gridModel = new Grid(GRID_SIZE);
        selectedPos = new ArrayList<>();
        selectedBtn = new ArrayList<>();
        loadIcons();
        player1 = chooseChampion(1);
        player2 = chooseChampion(2);
        currentPlayer = player1;
        opponent = player2;
        p1Gems = p2Gems = 0;
        checkSounds();
        initUI();
        // เริ่มต้นด้วยรอบแรก
        playSound("round1.wav");
    }

    private void loadIcons() {
        String[] states = {"idle","attack","defend","heal","skill"};
        String[] players = {"p1","p2"};
        String[] classes = {"Warrior","Mage","Rogue"};
        for (String who : players) {
            for (String cls : classes) {
                for (String st : states) {
                    String key = who + "_" + cls + "_" + st;
                    String path = String.format("/images/%s_%s_%s.png", who, cls, st);
                    URL u = getClass().getResource(path);
                    if (u != null) icons.put(key, new ImageIcon(u));
                    else if (DEV_MODE) System.err.println("Missing icon: " + path);
                }
            }
        }
    }

    private Entity chooseChampion(int num) {
        String[] champs = {"Warrior","Mage","Rogue"};
        String[] desc = {
            "Warrior: High Damage, High Defense",
            "Mage: High Magic Power, Low HP",
            "Rogue: High Speed, Crit Attacks"
        };
        int choice = JOptionPane.showOptionDialog(
            this,
            String.join("\n", desc),
            "Player " + num + ": Select Champion",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null, champs, champs[0]
        );
        String name = champs[Math.max(0, choice)];
        return switch (name) {
            case "Warrior" -> new Entity(name,120,50,5,Map.of(
                "Power Strike", new Skill("Power Strike",20,3),
                "Fortify",      new Skill("Fortify",15,4)
            ));
            case "Mage"    -> new Entity(name,80,80,7,Map.of(
                "Fireball",     new Skill("Fireball",25,3),
                "Arcane Shield",new Skill("Arcane Shield",20,5)
            ));
            case "Rogue"   -> new Entity(name,90,60,6,Map.of(
                "Quick Slash",  new Skill("Quick Slash",15,2),
                "Shadow Step",  new Skill("Shadow Step",30,4)
            ));
            default        -> new Entity(name,100,50,5,Map.of());
        };
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));
        Font f20 = new Font("SansSerif", Font.BOLD, 20);
        Font f24 = f20.deriveFont(24f);

        // TOP PANEL: Player1 | Center | Player2
        JPanel top = new JPanel(new GridLayout(1,3,10,10));
        top.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));

        // Player1
        JPanel p1Panel = new JPanel(new BorderLayout(5,5));
        p1Panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Player 1",
            TitledBorder.CENTER, TitledBorder.TOP, f24));
        p1ImgLabel = new JLabel(icons.get("p1_" + player1.name + "_idle"));
        p1Panel.add(p1ImgLabel, BorderLayout.WEST);
        JPanel p1Stats = new JPanel(new GridLayout(3,1,5,5));
        p1HpLabel   = new JLabel(); p1HpLabel.setFont(f20);
        p1ManaLabel = new JLabel(); p1ManaLabel.setFont(f20);
        p1GemsLabel = new JLabel(); p1GemsLabel.setFont(f20);
        p1Stats.add(p1HpLabel);
        p1Stats.add(p1ManaLabel);
        p1Stats.add(p1GemsLabel);
        p1Panel.add(p1Stats, BorderLayout.CENTER);
        top.add(p1Panel);

        // Center: Round & Score & Selection
        JPanel midPanel = new JPanel(new BorderLayout(5,5));
        midPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Selection",
            TitledBorder.CENTER, TitledBorder.TOP, f24));

        JPanel rs = new JPanel(new GridLayout(2,1));
        roundLabel = new JLabel("Round: 1 / 3", SwingConstants.CENTER);
        roundLabel.setFont(f20);
        scoreLabel = new JLabel("Score – P1: 0, P2: 0", SwingConstants.CENTER);
        scoreLabel.setFont(f20);
        rs.add(roundLabel);
        rs.add(scoreLabel);
        midPanel.add(rs, BorderLayout.NORTH);

        wordLabel       = new JLabel("Word: ", SwingConstants.CENTER);
        wordLabel.setFont(f20);
        dmgPreviewLabel = new JLabel("DMG Preview: 0", SwingConstants.CENTER);
        dmgPreviewLabel.setFont(f20);
        midPanel.add(wordLabel, BorderLayout.CENTER);
        midPanel.add(dmgPreviewLabel, BorderLayout.SOUTH);

        top.add(midPanel);

        // Player2
        JPanel p2Panel = new JPanel(new BorderLayout(5,5));
        p2Panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Player 2",
            TitledBorder.CENTER, TitledBorder.TOP, f24));
        p2ImgLabel = new JLabel(icons.get("p2_" + player2.name + "_idle"));
        p2Panel.add(p2ImgLabel, BorderLayout.EAST);
        JPanel p2Stats = new JPanel(new GridLayout(3,1,5,5));
        p2HpLabel   = new JLabel(); p2HpLabel.setFont(f20);
        p2ManaLabel = new JLabel(); p2ManaLabel.setFont(f20);
        p2GemsLabel = new JLabel(); p2GemsLabel.setFont(f20);
        p2Stats.add(p2HpLabel);
        p2Stats.add(p2ManaLabel);
        p2Stats.add(p2GemsLabel);
        p2Panel.add(p2Stats, BorderLayout.CENTER);
        top.add(p2Panel);

        add(top, BorderLayout.NORTH);

        // GRID PANEL
        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE,GRID_SIZE,2,2));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY,2));
        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        refreshGrid(gridPanel);
        add(gridPanel, BorderLayout.CENTER);

        // CONTROLS PANEL
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
        ctrl.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Controls",
            TitledBorder.CENTER, TitledBorder.TOP, f24));
        JButton bSubmit = new JButton("Submit");
        JButton bClear  = new JButton("Clear");
        JButton bShop   = new JButton("Shop");
        JButton bSkills = new JButton("Skills");
        for (JButton b : List.of(bSubmit,bClear,bShop,bSkills)) {
            b.setFont(f20);
            b.setPreferredSize(new Dimension(140,50));
        }
        bSubmit.addActionListener(e -> onSubmit(gridPanel));
        bClear .addActionListener(e -> clearSelection());
        bShop  .addActionListener(e -> openShop(gridPanel));
        bSkills.addActionListener(e -> openSkillDialog());
        ctrl.add(bSubmit);
        ctrl.add(bClear);
        ctrl.add(bShop);
        ctrl.add(bSkills);
        add(ctrl, BorderLayout.SOUTH);

        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        updateStatus();
    }

    private void refreshGrid(JPanel panel) {
        panel.removeAll();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                Tile t = gridModel.getTile(r, c);
                String html = String.format(
                    "<html><center>%c%s<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                    t.letter,
                    t.isSpecial() ? "<font color='red'>*</font>" : "",
                    t.getDmgPts(),
                    t.getGemPts()
                );
                JButton btn = new JButton(html);
                btn.setFont(new Font("Monospaced", Font.PLAIN, 24));
                btn.setBackground(t.isSpecial() ? Color.PINK : null);
                btn.putClientProperty("pos", new Position(r, c));
                btn.addActionListener(e -> onLetterClick((JButton) e.getSource()));
                buttons[r][c] = btn;
                panel.add(btn);
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private void onLetterClick(JButton btn) {
        Position p = (Position)btn.getClientProperty("pos");
    
        // ถ้าเลือกซ้ำ (ยกเลิกการเลือก)
        if (selectedPos.contains(p)) {
            int i = selectedPos.indexOf(p);
            if (i == selectedPos.size() - 1) {
                // รีเซ็ตสีพื้นกลับตามเดิมก่อนลบออก
                Tile t = gridModel.getTile(p.row, p.col);
                btn.setBackground(t.isSpecial() ? Color.PINK : null);
                selectedPos.remove(i);
                selectedBtn.remove(i);
            }
        } else {
            // ตรวจสอบว่าเป็นตัวอักษรถัดติดกัน
            if (!selectedPos.isEmpty()) {
                Position last = selectedPos.get(selectedPos.size() - 1);
                if (Math.abs(p.row - last.row) > 1 || Math.abs(p.col - last.col) > 1)
                    return;
            }
            selectedPos.add(p);
            selectedBtn.add(btn);
        }
    
        // เล่นเสียง select.wav ทุกครั้งที่คลิก
        playSound("select.wav");
    
        // ไฮไลต์ปุ่มที่ถูกเลือกใหม่
        // (ปุ่มที่ถูกยกเลิกไปจะมีสีกลับเป็นเดิมจากโค้ดข้างบน)
        selectedBtn.forEach(b -> b.setBackground(Color.YELLOW));
    
        // 3) คำนวณคำและ DMG Preview พร้อมรวม buffAttack
        StringBuilder sb = new StringBuilder();
        int dmg = 0;
        for (Position q : selectedPos) {
            Tile t = gridModel.getTile(q.row, q.col);
            sb.append(t.letter);
            dmg += t.getDmgPts();
            if (t.isSpecial()) dmg *= 2;
        }
        // รวมบัฟจากร้านค้า
        dmg += currentPlayer.buffAttack;
    
        wordLabel.setText("Word: " + sb);
        dmgPreviewLabel.setText("DMG Preview: " + dmg);
    }
    
    private void clearSelection() {
        for (JButton b : selectedBtn) {
            Position p = (Position) b.getClientProperty("pos");
            Tile t = gridModel.getTile(p.row, p.col);
            b.setBackground(t.isSpecial() ? Color.PINK : null);
        }
        selectedBtn.clear();
        selectedPos.clear();
        wordLabel.setText("Word: ");
        dmgPreviewLabel.setText("DMG Preview: 0");
    }

    private void onSubmit(JPanel gridPanel) {
        int totalDmg = 0; // Declare and initialize totalDmg
        if (opponent.shield) {
            playSound("defend.wav");           // เพิ่มตรงนี้
            totalDmg /= 2;
            opponent.shield = false;
            JOptionPane.showMessageDialog(this, opponent.name + " blocked half the damage!");
        }
        String w = wordLabel.getText().substring(6);
        if (w.length() < 3 || !dict.contains(w)) {
            JOptionPane.showMessageDialog(this, "Invalid word");
            clearSelection();
            return;
        }
        playSound("attack.wav");

        int base=0, gem=0;
        for (Position p : selectedPos) {
            Tile t = gridModel.getTile(p.row,p.col);
            base += t.getDmgPts();  gem  += t.getGemPts();
            if (t.isSpecial()) base *=2;
        }
        totalDmg = base + currentPlayer.buffAttack;
        if (opponent.shield) {
            totalDmg /=2; opponent.shield = false;
        }
        opponent.hp -= totalDmg;
        currentPlayer.buffAttack = 0;
        if (currentPlayer == player1) p1Gems += gem; else p2Gems += gem;

        JOptionPane.showMessageDialog(this,
            currentPlayer.name+" dealt "+totalDmg+" dmg, gained "+gem+" gems"
        );

        gridModel.removeAndCollapse(selectedPos);
        clearSelection();
        refreshGrid(gridPanel);

        // จบรอบถ้า HP คู่ต่อสู้ <= 0
        if (opponent.hp <= 0) {
            // อัปเดตคะแนน
            if (currentPlayer == player1) p1Wins++; else p2Wins++;
            scoreLabel.setText("Score – P1: "+p1Wins+", P2: "+p2Wins);

            // ถ้าจบรอบก่อนแมตช์
            if (p1Wins==2 || p2Wins==2 || currentRound==3) {
                playSound("Youwin.wav");
                String winner = p1Wins>p2Wins ? player1.name : player2.name;
                JOptionPane.showMessageDialog(this, winner+" wins the match!");
                System.exit(0);
            }

            // เริ่มรอบใหม่
            currentRound++;
            roundLabel.setText("Round: "+currentRound+" / 3");
            playSound("round"+currentRound+".wav");

            // รีเซ็ต HP/Mana/กริด
            player1.hp = player1.maxHp;  player1.mana = player1.maxMana;
            player2.hp = player2.maxHp;  player2.mana = player2.maxMana;
            gridModel = new Grid(GRID_SIZE);
            updateStatus();
        } else {
            // สลับเทิร์นปกติ
            nextTurn();
        }
    }

    private void openShop(JPanel gridPanel) {
        Entity e = currentPlayer;
        int gems = (e == player1 ? p1Gems : p2Gems);
        String[] opts = {"Heal (5)", "Shield (3)", "Buff (4)", "Shuffle (6)", "Mana (2)", "Close"};
        while (true) {
            int ch = JOptionPane.showOptionDialog(
                this,
                String.format("%s Gems: %d", e.name, gems),
                e.name + " Shop",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null, opts, opts[0]
            );
            if (ch < 0 || ch == opts.length - 1) break;
            int cost = Integer.parseInt(opts[ch].replaceAll("\\D", ""));
            if (gems < cost) {
                JOptionPane.showMessageDialog(this, "Not enough Gems");
                continue;
            }
            gems -= cost;
            switch (ch) {
                case 0: // Heal
                    e.hp = Math.min(e.maxHp, e.hp + 20);
                    animateAction("heal");
                    playSound("heal.wav");
                    JOptionPane.showMessageDialog(this, e.name + " healed 20 HP!");
                    break;
                case 1: // Shield
                    e.shield = true;
                    playSound("defend.wav");
                    animateAction("defend");
                    JOptionPane.showMessageDialog(this, e.name + " is shielded for 1 turn!");
                    break;
                case 2: // Buff
                    e.buffAttack += 10;
                    animateAction("skill");
                    JOptionPane.showMessageDialog(this, e.name + " gained +10 attack buff!");
                    break;
                case 3: // Shuffle
                    gridModel.shuffle();
                    refreshGrid(gridPanel);
                    JOptionPane.showMessageDialog(this, "Board shuffled!");
                    break;
                case 4: // Mana Potion
                    e.mana = Math.min(e.maxMana, e.mana + 30);
                    animateAction("heal");
                    playSound("heal.wav");
                    JOptionPane.showMessageDialog(this, e.name + " restored 30 Mana!");
                    break;
            }
            if (e == player1) p1Gems = gems;
            else            p2Gems = gems;
            updateStatus();
        }
    }

    private void openSkillDialog() {
        Entity u = currentPlayer, t = opponent;
        String[] skills = u.getSkillNames();
        String[] opts = new String[skills.length + 1];
        for (int i = 0; i < skills.length; i++) {
            Skill s = u.skills.get(skills[i]);
            opts[i] = String.format("%s (%d Mana, cd %d)", s.name, s.cost, s.cooldown);
        }
        opts[skills.length] = "Cancel";
    
        int ch = JOptionPane.showOptionDialog(
            this,
            String.format("Mana: %d/%d", u.mana, u.maxMana),
            u.name + " Skills",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null, opts, opts[0]
        );
        if (ch < 0 || ch >= skills.length) return;
    
        Skill s = u.skills.get(skills[ch]);
        if (u.mana < s.cost || s.remainingCd > 0) {
            JOptionPane.showMessageDialog(this, "Cannot use " + s.name + " now!");
            return;
        }
        u.mana -= s.cost;
        s.remainingCd = s.cooldown;
    
        // เล่นเสียงโจมตีหรือเวทตามอาชีพ
        if ("Mage".equals(u.name)) {
            playSound("magic.wav");
        } else {
            playSound("attack.wav");
        }
    
        String act, msg;
        switch (s.name) {
            case "Power Strike":
                t.hp -= 30; act = "attack";
                msg = u.name + " used Power Strike! -30 HP";
                break;
            case "Fortify":
                u.shield = true;
                playSound("defend.wav");
                act = "defend";
                msg = u.name + " cast Fortify! Shield up";
                break;
            case "Fireball":
                t.hp -= 40; act = "skill";
                msg = u.name + " cast Fireball! -40 HP";
                break;
            case "Arcane Shield":
                u.shield = true;
                playSound("defend.wav");
                act = "defend";
                msg = u.name + " used Arcane Shield! Shield up";
                break;
            case "Quick Slash":
                t.hp -= 20; act = "attack";
                msg = u.name + " used Quick Slash! -20 HP";
                break;
            case "Shadow Step":
                u.buffAttack += 20; act = "skill";
                msg = u.name + " used Shadow Step! +20 Attack";
                break;
            default:
                act = "skill";
                msg = u.name + " used " + s.name + "!";
        }
    
        animateAction(act);
        JOptionPane.showMessageDialog(this, msg);
        updateStatus();
        nextTurn();
    }

    private void animateAction(String action) {
        boolean is1=currentPlayer==player1;
        String who=is1?"p1":"p2";
        String cls=currentPlayer.name;
        JLabel lbl=is1?p1ImgLabel:p2ImgLabel;
        lbl.setIcon(icons.get(who+"_"+cls+"_"+action));
        new javax.swing.Timer(800,e->{
            lbl.setIcon(icons.get(who+"_"+cls+"_idle"));
            ((javax.swing.Timer)e.getSource()).stop();
        }).start();
    }

    private void nextTurn() {
        currentPlayer.regenMana();  currentPlayer.tickCooldowns();
        opponent.regenMana();       opponent.tickCooldowns();
        Entity tmp = currentPlayer; currentPlayer = opponent; opponent = tmp;
        updateStatus();
    }

    private void updateStatus() {
        p1HpLabel.setText("HP: "+player1.hp+"/"+player1.maxHp);
        p1ManaLabel.setText("Mana: "+player1.mana+"/"+player1.maxMana);
        p1GemsLabel.setText("Gems: "+p1Gems);
        p2HpLabel.setText("HP: "+player2.hp+"/"+player2.maxHp);
        p2ManaLabel.setText("Mana: "+player2.mana+"/"+player2.maxMana);
        p2GemsLabel.setText("Gems: "+p2Gems);
    }


    private void playSound(String fn) {
        // ตรวจสอบ resource ก่อน
        URL soundURL = getClass().getResource("/sounds/" + fn);
        if (soundURL == null) {
            if (DEV_MODE) System.err.println("Missing sound file: " + fn);
            return;
        }
        try (AudioInputStream in = AudioSystem.getAudioInputStream(soundURL)) {
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            clip.start();
        } catch (Exception e) {
            if (DEV_MODE) {
                System.err.println("Error playing sound: " + fn);
                e.printStackTrace();
            }
        }
    }

    private void checkSounds() {
        String[] required = {
            "fight.wav", "attack.wav", "magic.wav", "defend.wav",
            "round1.wav", "round2.wav", "round3.wav",
            "Youwin.wav", "select.wav"   // เพิ่ม select.wav
        };
        for (String fn : required) {
            if (getClass().getResource("/sounds/" + fn) == null) {
                System.err.println("Missing sound file: " + fn);
            }
        }
    }

    // === Nested classes ===
    public static class Entity {
        String name; int hp,maxHp; int mana,maxMana,mregen; int buffAttack; boolean shield;
        Map<String,Skill> skills;
        public Entity(String name,int mh,int mm,int mr,Map<String,Skill> s){
            this.name=name; this.maxHp=mh; this.hp=mh;
            this.maxMana=mm; this.mana=mm; this.mregen=mr;
            this.buffAttack=0; this.shield=false;
            this.skills=new HashMap<>(s);
        }
        public void regenMana(){ mana=Math.min(maxMana,mana+mregen); }
        public void tickCooldowns(){ for(Skill s:skills.values()) if(s.remainingCd>0) s.remainingCd--; }
        public String[] getSkillNames(){ return skills.keySet().toArray(new String[0]); }
    }
    public static class Skill {
        String name; int cost,cooldown,remainingCd;
        public Skill(String n,int c,int cd){ name=n; cost=c; cooldown=cd; remainingCd=0; }
    }
}
