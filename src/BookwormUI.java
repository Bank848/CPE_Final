// BookwormUI.java
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

    private void loadIcons() { // Load icons for the game
        String[] states = {"idle","attack","defend","heal"};
        for (String st: states) {
            icons.put("hero_"+st, new ImageIcon(getClass().getResource("./images/hero_"+st+".png")));
        }
        for (int lvl=1; lvl<=MAX_LEVEL; lvl++) {
            String keyBase = (lvl%5==0) ? "boss"+lvl : "enemy"+lvl;
            for (String st: states) {
                icons.put(keyBase+"_"+st,
                    new ImageIcon(getClass().getResource("./images/"+keyBase+"_"+st+".png")));
            }
        }
    }

    private void playSound(String soundFileName) {
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                getClass().getResource("./sounds/" + soundFileName))) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            if (DEV_MODE) e.printStackTrace();
        }
    }    

    private Entity createMonsterForLevel(int lvl) { // Create a monster based on the current level
        boolean isBoss = (lvl % 5 == 0);
        int baseHp = isBoss ? 80 + lvl*5 : 40 + lvl*3;
        return new Entity(isBoss ? "Boss Lv."+lvl : "Goblin Lv."+lvl, baseHp);
    }

    private void initGridPanel(JPanel gridPanel) {
        gridPanel.removeAll();
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
                btn.addActionListener(e -> onLetterClick((JButton) e.getSource()));
                buttons[r][c] = btn;
                gridPanel.add(btn);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel status = new JPanel(new GridLayout(2, 2, 5, 5));
        status.setBorder(BorderFactory.createTitledBorder("Status"));

        playerHpLabel = new JLabel(); monsterHpLabel = new JLabel();
        dmgPtLabel = new JLabel("Damage: 0"); gemLabel = new JLabel("Gems: 0");
        gemLabel.setFont(gemLabel.getFont().deriveFont(Font.BOLD, 14f));
        gemLabel.setForeground(new Color(128, 0, 128));

        status.add(playerHpLabel); status.add(monsterHpLabel);
        status.add(dmgPtLabel); status.add(gemLabel);
        playerImgLabel = new JLabel(icons.get("hero_idle"));
        monsterImgLabel = new JLabel(getMonsterIcon("idle"));
        top.add(playerImgLabel, BorderLayout.WEST);
        top.add(status, BorderLayout.CENTER);
        top.add(monsterImgLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 2, 2));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        initGridPanel(gridPanel);
        add(gridPanel, BorderLayout.CENTER);

        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        control.setBorder(BorderFactory.createTitledBorder("Controls"));
        wordLabel = new JLabel("Current: ");
        JButton submitBtn = new JButton("Submit"), clearBtn = new JButton("Clear"), shopBtn = new JButton("Shop");
        submitBtn.addActionListener(e -> onSubmit(gridPanel));
        clearBtn.addActionListener(e -> clearSelection());
        shopBtn.addActionListener(e -> openShop());
        control.add(wordLabel); control.add(submitBtn);
        control.add(clearBtn); control.add(shopBtn);
        if (DEV_MODE) {
            JButton skipBtn = new JButton("Skip Level");
            skipBtn.addActionListener(e -> nextLevel(gridPanel));
            control.add(skipBtn);
        }
        add(control, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 840);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private ImageIcon getMonsterIcon(String state) { // Get the icon for the monster based on its level and state
        String keyBase = (currentLevel%5==0) ? "boss"+currentLevel : "enemy"+currentLevel;
        return icons.get(keyBase+"_"+state);
    }

    private void updateStatusLabels() { // Update the status labels with current player and monster stats
        playerHpLabel .setText(player.name + " HP: "   + player.hp + "/" + player.maxHp);
        monsterHpLabel.setText(monster.name + " HP: "  + monster.hp + "/" + monster.maxHp);
        dmgPtLabel  .setText("Damage: " + totaldmgPts);
        gemLabel      .setText("Gems: "     + totalGems);
    }

    private void onLetterClick(JButton btn) { // Handle the letter button click event
        playSound("select.wav");
        Position p = (Position)btn.getClientProperty("pos");
        if (selectedPos.contains(p)) { 
            int idx = selectedPos.indexOf(p);
            if (idx == selectedPos.size() - 1) {
                selectedBtn.get(idx).setBackground(null);
                selectedPos.remove(idx);
                selectedBtn.remove(idx);
                updateWordLabel();
            }
            return;
        }
        if (!selectedPos.isEmpty()) {
            Position last = selectedPos.get(selectedPos.size()-1);
            if (Math.abs(p.row - last.row) > 1 || Math.abs(p.col - last.col) > 1) {
                return;
            }
        }
        selectedPos.add(p);
        selectedBtn.add(btn);
        btn.setBackground(Color.YELLOW);
        updateWordLabel();
    }

    private void updateWordLabel() { // Update the word label with the current selected letters
        StringBuilder sb = new StringBuilder();
        for (Position p : selectedPos) sb.append(gridModel.getTile(p.row,p.col).letter);
        wordLabel.setText("Current: " + sb.toString().toLowerCase());
    }

    private void onSubmit(JPanel gridPanel) {
        playSound("attack.wav");
        playerImgLabel.setIcon(icons.get("hero_attack"));
        SwingUtilities.invokeLater(() -> {
            try { Thread.sleep(300);} catch (Exception ign) {}
            performPlayerAction(gridPanel);
        });
    }

    private void performPlayerAction(JPanel gridPanel) {
        StringBuilder sb = new StringBuilder();
        boolean specialUsed = false;
        for (Position p : selectedPos) {
            Tile t = gridModel.getTile(p.row, p.col);
            sb.append(t.letter);
            if (t.isSpecial()) specialUsed = true;
        }
        String word = sb.toString().toLowerCase();
        if (word.length() < 3 || !dict.contains(word)) {
            JOptionPane.showMessageDialog(this, "Word invalid or too short.");
            playerImgLabel.setIcon(icons.get("hero_idle"));
        } else {
            int gaindmg = 0, gainG = 0;
            for (Position p : selectedPos) {
                Tile t = gridModel.getTile(p.row, p.col);
                gaindmg += t.getDmgPts(); gainG += t.getGemPts();
            }
            if (specialUsed) {
                gaindmg *= 2;
                JOptionPane.showMessageDialog(this, "Special tile! Damage doubled and board will reset.");
            }
            totaldmgPts += gaindmg;
            totalGems += gainG;
            monster.hp -= gaindmg + player.buffAttack;
            JOptionPane.showMessageDialog(this,
                String.format("You dealt %d dmg, +%d Gems", gaindmg, gainG));
            new Timer(1000, e -> { playerImgLabel.setIcon(icons.get("hero_idle")); ((Timer)e.getSource()).stop(); }).start();
            if (specialUsed) {
                gridModel = new Grid(GRID_SIZE);
            } else {
                gridModel.removeAndCollapse(selectedPos);
            }
            initGridPanel(gridPanel);
            updateStatusLabels();
            if (monster.hp <= 0) { nextLevel(gridPanel); return; }
            enemyTurn();
        }
        clearSelection(); updateStatusLabels();
    }

    private void enemyTurn() { // Handle the enemy's turn after the player attacks
        monsterImgLabel.setIcon(getMonsterIcon("attack"));
        SwingUtilities.invokeLater(() -> {
            try { Thread.sleep(1000); } catch (Exception ign) {}
            performEnemyAction();
        });
    }

    private void performEnemyAction() { // Perform the monster's action
        Random rand = new Random();
        boolean isBoss = (currentLevel % 5 == 0);
        double chance = rand.nextDouble();
    
        if ((isBoss && chance < 0.25) || (!isBoss && chance < 0.10)) { // 25% chance to heal // 10% for normal
            monster.hp = Math.min(monster.maxHp, monster.hp + 15);
            String healSfx = isBoss ? "boss_heal.wav" : "enemy_heal.wav";
            playSound(healSfx);
            monsterImgLabel.setIcon(getMonsterIcon("heal"));
            new Timer(1000, e -> {
                monsterImgLabel.setIcon(getMonsterIcon("idle"));
                ((Timer)e.getSource()).stop();
            }).start();
            JOptionPane.showMessageDialog(this, monster.name + " healed itself for 15 HP!");
    
        } else if ((isBoss && chance < 0.50) || (!isBoss && chance < 0.20)) { // 50% chance to defend // 20% for normal
            monster.setShield(true);
            String defSfx = isBoss ? "boss_defend.wav" : "enemy_defend.wav";
            playSound(defSfx);
            monsterImgLabel.setIcon(getMonsterIcon("defend"));
            new Timer(1000, e -> {
                monsterImgLabel.setIcon(getMonsterIcon("idle"));
                ((Timer)e.getSource()).stop();
            }).start();
            JOptionPane.showMessageDialog(this, monster.name + " is defending!");
    
        } else {
            int len = 3 + rand.nextInt(3);
            int dmg = 0;
            for (int i = 0; i < len; i++) {
                Tile t = gridModel.getTile(rand.nextInt(GRID_SIZE), rand.nextInt(GRID_SIZE));
                dmg += t.getDmgPts(); 
            }
    
            if (monster.isShieldActive()) { // Monster's shield active
                dmg /= 2;
                monster.setShield(false);
                JOptionPane.showMessageDialog(this,
                    String.format("%s's shield halved the damage!", monster.name));
            }
    
            if (shieldActive) { // Player's shield active
                dmg /= 2;
                shieldActive = false;
                JOptionPane.showMessageDialog(this,
                    String.format("Your shield blocked 50%%! You took %d dmg.", dmg));
            }
    
            player.hp -= dmg;
            String atkSfx = isBoss ? "boss_attack.wav" : "enemy_attack.wav";
            playSound(atkSfx);
            monsterImgLabel.setIcon(getMonsterIcon("attack"));
            new Timer(1000, e -> {
                monsterImgLabel.setIcon(getMonsterIcon("idle"));
                ((Timer)e.getSource()).stop();
            }).start();
            JOptionPane.showMessageDialog(this,
                String.format("%s dealt %d dmg to you!", monster.name, dmg));
        }
    
        updateStatusLabels();
        if (player.hp <= 0) {
            JOptionPane.showMessageDialog(this, "You have been defeated...");
            System.exit(0);
        }
    }

    private void clearSelection() {
        for (JButton b : selectedBtn) b.setBackground(null);
        selectedBtn.clear(); selectedPos.clear();
        wordLabel.setText("Current: ");
    }


    private void refreshGrid() { // Refresh the grid UI after changes
        for (int r=0; r<GRID_SIZE; r++) {
            for (int c=0; c<GRID_SIZE; c++) {
                Tile t = gridModel.getTile(r,c);
                buttons[r][c].setText(String.format(
                    "<html><center>%c<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                    t.letter, t.getDmgPts(), t.getGemPts()));
            }
        }
    }

    private void nextLevel(JPanel gridPanel) {
        currentLevel++;
        if (currentLevel > MAX_LEVEL) { JOptionPane.showMessageDialog(this, "You WIN!"); System.exit(0); }
        monster = createMonsterForLevel(currentLevel);
        monsterImgLabel.setIcon(getMonsterIcon("idle"));
        JOptionPane.showMessageDialog(this, "Level up! Now facing " + monster.name);
        gridModel = new Grid(GRID_SIZE);
        initGridPanel(gridPanel); clearSelection(); updateStatusLabels();
    }

    private void openShop() { // Open the shop to buy items
        String[] options = {
            "Heal (5 Gems)",
            "Shield (3 Gems)",
            "BuffAtk (4 Gems)",
            "Shuffle (6 Gems)",
            "Close"
        };
        while (true) {
            int choice = JOptionPane.showOptionDialog(this,
                "Gems: " + totalGems + "\nSelect Item to buy", "Shop",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
            if (choice < 0 || choice == options.length - 1) break;
            switch (choice) {
                case 0: // Heal
                    if (totalGems >= 5) {
                        totalGems -= 5;
                        player.hp = Math.min(player.maxHp, player.hp + 20);
                        playSound("heal.wav");
                        playerImgLabel.setIcon(icons.get("hero_heal"));
                        new Timer(1000, e -> {
                            playerImgLabel.setIcon(icons.get("hero_idle"));
                            ((Timer)e.getSource()).stop();
                        }).start();
    
                        JOptionPane.showMessageDialog(this, "Healed 20 HP!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Not enough Gems");
                    }
                    break;
                case 1: // Shield
                    if (totalGems >= 3) {
                        totalGems -= 3;
                        shieldActive = true;
                        playSound("defend.wav"); 
                        playerImgLabel.setIcon(icons.get("hero_defend"));
                        new Timer(1000, e -> {
                            playerImgLabel.setIcon(icons.get("hero_idle"));
                            ((Timer)e.getSource()).stop();
                        }).start();
    
                        JOptionPane.showMessageDialog(this, "Shield ready! Next attack will be reduced.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Not enough Gems");
                    }
                    break;
                case 2: // Buff Attack
                    if (totalGems>=4) {
                        totalGems -= 4;
                        player.buffAttack += 10;
                        JOptionPane.showMessageDialog(this, "Attack +10 next!");
                    } else JOptionPane.showMessageDialog(this, "Not enough Gems");
                    break;
                case 3: // Shuffle
                    if (totalGems>=6) {
                        totalGems -= 6;
                        gridModel.shuffle();
                        refreshGrid();
                        clearSelection();
                        JOptionPane.showMessageDialog(this, "Board shuffled!");
                    } else JOptionPane.showMessageDialog(this, "Not enough Gems");
                    break;
            }
            updateStatusLabels();
        }
    }

    public static void main(String[] args) { // Main method to run the game
        SwingUtilities.invokeLater(BookwormUI::new);
    }
}
