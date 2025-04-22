// BookwormUI.java
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class BookwormUI extends JFrame {
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
    private int totalBlackPts = 0;
    private int totalGems     = 0;    // เปลี่ยนชื่อเป็น Gems
    private boolean shieldActive = false;

    private JLabel wordLabel, blackPtLabel, gemLabel;    // เปลี่ยน bluePtLabel → gemLabel
    private JLabel playerHpLabel, monsterHpLabel;
    private JLabel playerImgLabel, monsterImgLabel;

    private Map<String, ImageIcon> icons = new HashMap<>();

    public BookwormUI() {
        super("Bookworm Puzzle RPG");
        loadIcons();
        gridModel    = new Grid(GRID_SIZE);
        dict         = new FreeDictionary();
        selectedPos  = new ArrayList<>();
        selectedBtn  = new ArrayList<>();
        player       = new Entity("Hero", 100);
        monster      = createMonsterForLevel(currentLevel);

        initUI();
        updateStatusLabels();
    }

    private void loadIcons() {
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

    private Entity createMonsterForLevel(int lvl) {
        boolean isBoss = (lvl % 5 == 0);
        int baseHp = isBoss ? 80 + lvl*5 : 40 + lvl*3;
        return new Entity(isBoss ? "Boss Lv."+lvl : "Goblin Lv."+lvl, baseHp);
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));

        // === Top Panel ===
        JPanel top = new JPanel(new BorderLayout(10,10));
        top.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JPanel status = new JPanel(new GridLayout(2,2,5,5));
        status.setBorder(BorderFactory.createTitledBorder("Status"));

        playerHpLabel  = new JLabel();
        monsterHpLabel = new JLabel();
        blackPtLabel   = new JLabel("Black Pts: 0");
        gemLabel       = new JLabel("Gems: 0");

        // ตกแต่งสีและฟอนต์
        gemLabel.setFont(gemLabel.getFont().deriveFont(Font.BOLD, 14f));
        gemLabel.setForeground(new Color(128,0,128));  // สี Purple

        status.add(playerHpLabel);
        status.add(monsterHpLabel);
        status.add(blackPtLabel);
        status.add(gemLabel);

        playerImgLabel  = new JLabel(icons.get("hero_idle"));
        monsterImgLabel = new JLabel(getMonsterIcon("idle"));

        top.add(playerImgLabel,   BorderLayout.WEST);
        top.add(status,           BorderLayout.CENTER);
        top.add(monsterImgLabel,  BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // === Center Grid ===
        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE,2,2));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        for (int r=0; r<GRID_SIZE; r++){
            for (int c=0; c<GRID_SIZE; c++){
                Tile t = gridModel.getTile(r,c);
                JButton btn = new JButton(String.format(
                    "<html><center>%c<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                    t.letter, t.blackPts, t.bluePts));
                btn.setFont(new Font("Monospaced", Font.PLAIN, 16));
                btn.putClientProperty("pos", new Position(r,c));
                btn.addActionListener(e -> onLetterClick((JButton)e.getSource()));
                buttons[r][c] = btn;
                gridPanel.add(btn);
            }
        }
        add(gridPanel, BorderLayout.CENTER);

        // === Bottom Controls ===
        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        control.setBorder(BorderFactory.createTitledBorder("Controls"));

        wordLabel  = new JLabel("Current: ");
        JButton submitBtn = new JButton("Submit");
        JButton clearBtn  = new JButton("Clear");
        JButton shopBtn   = new JButton("Shop");
        submitBtn.addActionListener(e -> onSubmit());
        clearBtn .addActionListener(e -> clearSelection());
        shopBtn  .addActionListener(e -> openShop());

        control.add(wordLabel);
        control.add(submitBtn);
        control.add(clearBtn);
        control.add(shopBtn);

        if (DEV_MODE) {
            JButton skipBtn = new JButton("Skip Level");
            skipBtn.addActionListener(e -> nextLevel());
            control.add(skipBtn);
        }

        add(control, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 840);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private ImageIcon getMonsterIcon(String state) {
        String keyBase = (currentLevel%5==0) ? "boss"+currentLevel : "enemy"+currentLevel;
        return icons.get(keyBase+"_"+state);
    }

    private void updateStatusLabels() {
        playerHpLabel .setText(player.name + " HP: "   + player.hp + "/" + player.maxHp);
        monsterHpLabel.setText(monster.name + " HP: "  + monster.hp + "/" + monster.maxHp);
        blackPtLabel  .setText("Black Pts: " + totalBlackPts);
        gemLabel      .setText("Gems: "     + totalGems);
    }

    private void onLetterClick(JButton btn) {
        Position p = (Position)btn.getClientProperty("pos");
        if (!selectedPos.isEmpty()) {
            Position last = selectedPos.get(selectedPos.size()-1);
            if (Math.abs(p.row-last.row)>1 || Math.abs(p.col-last.col)>1) return;
        }
        if (selectedPos.contains(p)) return;
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

    private void onSubmit() {
        playerImgLabel.setIcon(icons.get("hero_attack"));
        SwingUtilities.invokeLater(() -> {
            try { Thread.sleep(300); } catch (Exception ign) {}
            performPlayerAction();
        });
    }

    private void performPlayerAction() {
        StringBuilder sb = new StringBuilder();
        for (Position p : selectedPos) sb.append(gridModel.getTile(p.row,p.col).letter);
        String word = sb.toString().toLowerCase();
        if (word.length() < 3) {
            JOptionPane.showMessageDialog(this, "Word too short!");
            playerImgLabel.setIcon(icons.get("hero_idle"));
        } else if (!dict.contains(word)) {
            JOptionPane.showMessageDialog(this, "\""+ word +"\" is not valid.");
            playerImgLabel.setIcon(icons.get("hero_idle"));
        } else {
            int gainB = 0, gainG = 0;
            for (Position p : selectedPos) {
                Tile t = gridModel.getTile(p.row, p.col);
                gainB += t.blackPts;
                gainG += t.bluePts;
            }
            totalBlackPts += gainB;
            totalGems    += gainG;
            monster.hp   -= gainB + player.buffAttack;
            
            // แสดงข้อความให้ใช้ G แทน L (L = bluePts เดิม)
            JOptionPane.showMessageDialog(this,
                String.format("You dealt %d dmg, +%d Black, +%d Gems", gainB, gainB, gainG));
                
            gridModel.removeAndCollapse(selectedPos);
            refreshGrid();
            updateStatusLabels();
            if (monster.hp<=0) {
                nextLevel();
                return;
            }
            enemyTurn();
        }
        clearSelection();
        updateStatusLabels();
    }

    private void enemyTurn() {
        monsterImgLabel.setIcon(getMonsterIcon("attack"));
        SwingUtilities.invokeLater(() -> {
            try { Thread.sleep(500); } catch (Exception ign) {}
            performEnemyAction();
        });
    }

    private void performEnemyAction() {
        int len = 3 + new Random().nextInt(3);
        int gained = 0;
        Random r = new Random();
        for (int i=0; i<len; i++){
            Tile t = gridModel.getTile(r.nextInt(GRID_SIZE), r.nextInt(GRID_SIZE));
            gained += t.blackPts;
        }

        if (shieldActive) {
            int dmg = gained / 2;
            player.hp -= dmg;
            JOptionPane.showMessageDialog(this,
                String.format("Shield reduced the attack by 50%%! You took %d dmg.", dmg));
            shieldActive = false;
        } else {
            player.hp -= gained;
            JOptionPane.showMessageDialog(this,
                String.format("%s dealt %d dmg to you!", monster.name, gained));
        }

        monsterImgLabel.setIcon(getMonsterIcon("idle"));
        playerImgLabel.setIcon(icons.get("hero_idle"));
        updateStatusLabels();
        if (player.hp<=0) {
            JOptionPane.showMessageDialog(this, "You have been defeated...");
            System.exit(0);
        }
    }

    private void clearSelection() {
        for (JButton b: selectedBtn) b.setBackground(null);
        selectedBtn.clear();
        selectedPos.clear();
        wordLabel.setText("Current: ");
    }

    private void refreshGrid() {
        for (int r=0; r<GRID_SIZE; r++) {
            for (int c=0; c<GRID_SIZE; c++) {
                Tile t = gridModel.getTile(r,c);
                buttons[r][c].setText(String.format(
                    "<html><center>%c<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                    t.letter, t.blackPts, t.bluePts));
            }
        }
    }

    private void nextLevel() {
        currentLevel++;
        if (currentLevel>MAX_LEVEL) {
            JOptionPane.showMessageDialog(this, "You WIN! 🎉");
            System.exit(0);
        }
        monster = createMonsterForLevel(currentLevel);
        JOptionPane.showMessageDialog(this,
            String.format("Level up! Now facing %s", monster.name));
        monsterImgLabel.setIcon(getMonsterIcon("idle"));
        gridModel = new Grid(GRID_SIZE);
        for (int r=0; r<GRID_SIZE; r++) for (int c=0; c<GRID_SIZE; c++) {
            Tile t = gridModel.getTile(r,c);
            buttons[r][c].setText(String.format(
                "<html><center>%c<br><font color='black'>%d</font> / <font color='blue'>%d</font></center></html>",
                t.letter, t.blackPts, t.bluePts));
        }
        clearSelection();
        updateStatusLabels();
    }

    private void openShop() {
        String[] options = {
            "Heal (5 Gems)",
            "Shield (3 Gems)",
            "BuffAtk (4 Gems)",
            "Shuffle (6 Gems)",    // เพิ่มตัวเลือก
            "Close"
        };
        while (true) {
            int choice = JOptionPane.showOptionDialog(this,
                "Gems: " + totalGems + "\nSelect Item to buy", "Shop",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
            if (choice < 0 || choice == options.length - 1) break;
            switch (choice) {
                case 0:
                    if (totalGems>=5) {
                        totalGems -= 5;
                        player.hp = Math.min(player.maxHp, player.hp + 20);
                        JOptionPane.showMessageDialog(this, "Healed 20 HP!");
                    } else JOptionPane.showMessageDialog(this, "Not enough Gems");
                    break;
                case 1:
                    if (totalGems>=3) {
                        totalGems -= 3;
                        shieldActive = true;
                        JOptionPane.showMessageDialog(this, "Shield ready! Next attack will be reduced.");
                    } else JOptionPane.showMessageDialog(this, "Not enough Gems");
                    break;
                case 2:
                    if (totalGems>=4) {
                        totalGems -= 4;
                        player.buffAttack += 10;
                        JOptionPane.showMessageDialog(this, "Attack +10 next!");
                    } else JOptionPane.showMessageDialog(this, "Not enough Gems");
                    break;
                case 3:
                    if (totalGems>=6) {
                        totalGems -= 6;
                        gridModel.shuffle();    // สุ่มกระดานใหม่ทั้งหมด
                        refreshGrid();
                        clearSelection();
                        JOptionPane.showMessageDialog(this, "Board shuffled!");
                    } else JOptionPane.showMessageDialog(this, "Not enough Gems");
                    break;
            }
            updateStatusLabels();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BookwormUI::new);
    }
}
