import java.awt.*;
import java.util.*; // ใช้ ArrayList, List, Map, HashMap, Queue, LinkedList
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.Timer;

public class BookwormUI extends JFrame { // Main UI class for Bookworm Puzzle RPG
    private static final int GRID_SIZE = 8;
    private static final int MAX_LEVEL = 20;
    private static final boolean DEV_MODE = true;
    private static final int HP_INCREMENT = 4;
    private static final double ATTACK_MULTIPLIER = 1.25;
    private static final int ATTACK_CAP = 40;
    private final Random rand = new Random();

    private Grid gridModel;
    private FreeDictionary dict;
    private JButton[][] buttons;
    private List<Position> selectedPos;
    private List<JButton> selectedBtn;

    private Entity player;
    private Entity monster;
    private int currentLevel = 1;
    private int totaldmgPts = 0;
    private int totalGems = 0;
    private boolean shieldActive = false;

    private JLabel wordLabel, dmgPtLabel, gemLabel;
    private JLabel playerHpLabel, monsterHpLabel;
    private JLabel playerImgLabel, monsterImgLabel;

    private Map<String, ImageIcon> icons = new HashMap<>();
    protected JPanel gridPanel;
    private enum Reaction { HEAL, DEFEND, COUNTER }


    public BookwormUI() {
        super("Bookworm Puzzle RPG");
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
        for (String st: states) {
            icons.put("hero_" + st,
                new ImageIcon(getClass().getResource("./images/hero_" + st + ".png")));
        }
        for (int lvl = 1; lvl <= MAX_LEVEL; lvl++) {
            String keyBase = (lvl % 5 == 0) ? "boss" + lvl : "enemy" + lvl;
            for (String st: states) {
                icons.put(keyBase + "_" + st,
                    new ImageIcon(getClass().getResource("./images/" + keyBase + "_" + st + ".png")));
            }
        }
    }

    public void playSound(String soundFileName) {
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                getClass().getResource("./sounds/" + soundFileName))) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            if (DEV_MODE) e.printStackTrace();
        }
    }

    private Entity createMonsterForLevel(int lvl) {
        boolean isBoss = (lvl % 5 == 0);
        int baseHp;
        if (isBoss) {
            Entity prevGoblin = createMonsterForLevel(lvl - 1);
            baseHp = prevGoblin.maxHp * 2;
        } else if (lvl > 1) {
            Entity prev = createMonsterForLevel(lvl - 1);
            if ((lvl - 1) % 5 == 0) baseHp = prev.maxHp;
            else baseHp = prev.maxHp + HP_INCREMENT;
        } else {
            baseHp = 40 + lvl * 3;
        }
        String name = isBoss ? "Boss Lv." + lvl : "Goblin Lv." + lvl;
        return new Entity(name, baseHp);
    }

    private void initGridPanel(JPanel panel) {
        panel.removeAll();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                Tile t = gridModel.getTile(r, c);
                JButton btn = new JButton();
                String html = String.format(
                    "<html><center>%c%s<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                    t.letter,
                    t.isSpecial() ? "<font color='red'>*</font>" : "",
                    t.getDmgPts(), t.getGemPts());
                btn.setText(html);
                if (t.isSpecial()) btn.setBackground(Color.PINK);
                btn.setFont(new Font("Monospaced", Font.PLAIN, 16));
                btn.putClientProperty("pos", new Position(r, c));
                btn.addActionListener(e -> onLetterClick((JButton)e.getSource()));
                buttons[r][c] = btn;
                panel.add(btn);
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));
        JPanel top = new JPanel(new BorderLayout(10,10));
        top.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JPanel status = new JPanel(new GridLayout(2,2,5,5));
        status.setBorder(BorderFactory.createTitledBorder("Status"));
        playerHpLabel = new JLabel();
        monsterHpLabel = new JLabel();
        dmgPtLabel = new JLabel("Damage: 0");
        gemLabel = new JLabel("Gems: 0");
        gemLabel.setFont(gemLabel.getFont().deriveFont(Font.BOLD,14f));
        gemLabel.setForeground(new Color(128,0,128));
        status.add(playerHpLabel);
        status.add(monsterHpLabel);
        status.add(dmgPtLabel);
        status.add(gemLabel);
        playerImgLabel = new JLabel(icons.get("hero_idle"));
        monsterImgLabel = new JLabel(getMonsterIcon("idle"));
        top.add(playerImgLabel, BorderLayout.WEST);
        top.add(status, BorderLayout.CENTER);
        top.add(monsterImgLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        gridPanel = new JPanel(new GridLayout(GRID_SIZE,GRID_SIZE,2,2));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY,2));
        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        initGridPanel(gridPanel);
        add(gridPanel, BorderLayout.CENTER);

        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
        control.setBorder(BorderFactory.createTitledBorder("Controls"));
        wordLabel = new JLabel("Current: ");
        JButton submitBtn = new JButton("Submit");
        JButton clearBtn = new JButton("Clear");
        JButton shopBtn = new JButton("Shop");
        submitBtn.addActionListener(e -> onSubmit(gridPanel));
        clearBtn.addActionListener(e -> clearSelection());
        shopBtn.addActionListener(e -> openShop());
        control.add(wordLabel);
        control.add(submitBtn);
        control.add(clearBtn);
        control.add(shopBtn);
        if (DEV_MODE) {
            JButton skipBtn = new JButton("Skip Level");
            skipBtn.addActionListener(e -> nextLevel(gridPanel));
            control.add(skipBtn);
        }
        add(control, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820,840);
        setLocationRelativeTo(null);
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
        String keyBase = (currentLevel%5==0) ? "boss"+currentLevel : "enemy"+currentLevel;
        return icons.get(keyBase+"_"+state);
    }

    private void updateStatusLabels() {
        playerHpLabel.setText(player.name+" HP: "+player.hp+"/"+player.maxHp);
        monsterHpLabel.setText(monster.name+" HP: "+monster.hp+"/"+monster.maxHp);
        dmgPtLabel.setText("Damage: "+totaldmgPts);
        gemLabel.setText("Gems: "+totalGems);
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
        for (Position p : selectedPos) sb.append(gridModel.getTile(p.row,p.col).letter);
        wordLabel.setText("Current: " + sb.toString().toLowerCase());
    }

    private void onSubmit(JPanel grid) {
        playSound("attack.wav");
        playerImgLabel.setIcon(icons.get("hero_attack"));
        SwingUtilities.invokeLater(() -> {
            try { Thread.sleep(300);} catch (Exception ignored) {}
            performPlayerAction(grid);
        });
    }

    // Decide reaction for monster's immediate response
    private Reaction decideReaction() {
        boolean isBoss = (currentLevel % 5 == 0);
        double ch = rand.nextDouble();
        double ht = isBoss?0.25:0.10;
        double dt = isBoss?0.50:0.25;
        if (ch < ht) return Reaction.HEAL;
        else if (ch < dt) return Reaction.DEFEND;
        else return Reaction.COUNTER;
    }

    /**
     * Generate random damage based on picking 3-5 random tiles
     */
    private int calculateRandomDamage() {
        int len = 3 + rand.nextInt(3);
        int sum = 0;
        for (int i=0;i<len;i++) sum += gridModel.getTile(rand.nextInt(GRID_SIZE),rand.nextInt(GRID_SIZE)).getDmgPts();
        int scaled = (int)(sum * ATTACK_MULTIPLIER);
        return Math.min(scaled, ATTACK_CAP);
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
            int gaindmg=0, gainG=0;
            for (Position p: selectedPos) {
                Tile t = gridModel.getTile(p.row,p.col);
                gaindmg += t.getDmgPts(); gainG += t.getGemPts();
            }
            if (special) { gaindmg *= 2; JOptionPane.showMessageDialog(this,"Special tile! Damage doubled and board will reset."); }
            totaldmgPts += gaindmg; totalGems += gainG;

            Reaction react = decideReaction();
            int raw = gaindmg + player.buffAttack;

            // DEFEND reaction: give monster shield to block this hit
            if (react == Reaction.DEFEND) {
                monster.setShield(true);
                playSound((currentLevel%5==0)?"boss_defend.wav":"enemy_defend.wav");
                monsterImgLabel.setIcon(getMonsterIcon("defend"));
                new Timer(500,e->{ monsterImgLabel.setIcon(getMonsterIcon("idle")); ((Timer)e.getSource()).stop(); }).start();
            }

            // apply shield if any
            if (monster.isShieldActive()) {
                raw /= 2;
                monster.setShield(false);
                JOptionPane.showMessageDialog(this, monster.name+" blocks 50% of your damage!");
            }

            // deal damage
            monster.hp -= raw;
            JOptionPane.showMessageDialog(this,String.format("You dealt %d dmg, +%d Gems", raw, gainG));
            new Timer(1000,e->{ playerImgLabel.setIcon(icons.get("hero_idle")); ((Timer)e.getSource()).stop(); }).start();

            // grid update
            if (special) gridModel = new Grid(GRID_SIZE);
            else gridModel.removeAndCollapse(selectedPos);
            initGridPanel(grid);
            updateStatusLabels();

            if (monster.hp>0) reactToPlayerAttack(react);
            else nextLevel(grid);
        }
        clearSelection(); updateStatusLabels();
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
                int dmg = calculateRandomDamage();
                if (shieldActive) { dmg/=2; shieldActive=false; JOptionPane.showMessageDialog(this,"Your shield blocks 50% of the counterattack!"); }
                player.hp -= dmg;
                playSound(isBoss?"boss_attack.wav":"enemy_attack.wav");
                monsterImgLabel.setIcon(getMonsterIcon("attack"));
                new Timer(1000,e->{ monsterImgLabel.setIcon(getMonsterIcon("idle")); ((Timer)e.getSource()).stop(); }).start();
                JOptionPane.showMessageDialog(this, monster.name+" counterattacks for "+dmg+" damage!");
                if (player.hp<=0) { playSound("gameover.wav"); JOptionPane.showMessageDialog(this,"You have been defeated..."); System.exit(0);}                
                break;
        }
        updateStatusLabels();
    }

    public void clearSelection() {
        for (JButton b : selectedBtn) b.setBackground(null);
        selectedBtn.clear(); selectedPos.clear();
        wordLabel.setText("Current: ");
    }

    private void refreshGrid() {
        for (int r=0; r<GRID_SIZE; r++) for (int c=0; c<GRID_SIZE; c++) {
            Tile t = gridModel.getTile(r,c);
            buttons[r][c].setText(String.format("<html><center>%c<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                t.letter, t.getDmgPts(), t.getGemPts()));
        }
    }

    private void nextLevel(JPanel gridPanel) {
        currentLevel++;
        if (currentLevel>MAX_LEVEL) { JOptionPane.showMessageDialog(this,"You WIN!"); System.exit(0);}        
        monster = createMonsterForLevel(currentLevel);
        monsterImgLabel.setIcon(getMonsterIcon("idle"));
        JOptionPane.showMessageDialog(this,"Level up! Now facing "+monster.name);
        gridModel = new Grid(GRID_SIZE);
        initGridPanel(gridPanel); clearSelection(); updateStatusLabels();
    }

    private void openShop() { openShopForPlayer(player); }
    public void openShopForPlayer(Entity e) {
        String[] opts = {"Heal (5 Gems)","Shield (3 Gems)","BuffAtk (4 Gems)","Shuffle (6 Gems)","Close"};
        while (true) {
            int c = JOptionPane.showOptionDialog(this,"Gems: "+totalGems+"\nSelect Item to buy","Shop",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,null,opts,opts[0]);
            if (c<0||c==opts.length-1) break;
            switch(c) {
                case 0: if (totalGems>=5){ totalGems-=5; e.hp=Math.min(e.maxHp,e.hp+20); playSound("heal.wav"); } else JOptionPane.showMessageDialog(this,"Not enough Gems"); break;
                case 1: if (totalGems>=3){ totalGems-=3; shieldActive=true; playSound("defend.wav"); } else JOptionPane.showMessageDialog(this,"Not enough Gems"); break;
                case 2: if (totalGems>=4){ totalGems-=4; e.buffAttack+=10; } else JOptionPane.showMessageDialog(this,"Not enough Gems"); break;
                case 3: if (totalGems>=6){ totalGems-=6; gridModel.shuffle(); refreshGrid(); clearSelection(); } else JOptionPane.showMessageDialog(this,"Not enough Gems"); break;
            }
        }
        updateStatusLabels();
    }
}
