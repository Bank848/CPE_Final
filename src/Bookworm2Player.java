import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Bookworm2Player extends JFrame {
    private static final int GRID_SIZE = 8;
    private static final int MAX_PURCHASES = 3;
    private static final boolean DEV_MODE = true;

    private Grid gridModel;
    private FreeDictionary dict;
    private JButton[][] buttons;
    private List<Position> selectedPos;
    private List<JButton> selectedBtn;

    private Entity player1, player2;
    private Entity currentPlayer, opponent;

    private int p1Gems, p2Gems;
    private JLabel wordLabel;
    private JLabel p1HpLabel, p2HpLabel;
    private JLabel p1GemsLabel, p2GemsLabel;
    private JLabel p1ImgLabel, p2ImgLabel;
    private JLabel roundLabel;

    private Map<String, ImageIcon> icons = new HashMap<>();
    private int roundCount = 1;

    public Bookworm2Player() {
        super("Bookworm 2 Player Mode");
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
        initUI();
        playSound("fight.wav");
    }

    private Entity chooseChampion(int num) {
        playSound("select.wav");
        String[] champs = {"Warrior", "Mage", "Rogue"};
        String[] desc = {
            "Warrior: High Damge Defense",
            "Mage: High Magic Power, Casts Spells",
            "Rogue: High Speed, Critical Attacks",
        };
        int choice = JOptionPane.showOptionDialog(
            this,
            String.join("\n", desc),
            "Player " + num + ": Select Your Champion",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            champs,
            champs[0]
        );
        String name = champs[choice < 0 ? 0 : choice];
        return new Entity(name, 100);
    }

    private void loadIcons() {
        String[] states = {"idle", "attack", "defend", "heal"};
        for (String who : new String[]{"p1", "p2"}) {
            for (String st : states) {
                String path = "/images/" + who + "_" + st + ".png";
                URL imgURL = getClass().getResource(path);
                if (imgURL != null) icons.put(who + "_" + st, new ImageIcon(imgURL));
                else if (DEV_MODE) System.err.println("Icon missing: " + path);
            }
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));
        JPanel top = new JPanel(new BorderLayout(10,10));
        top.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Status & Images
        JPanel status = new JPanel(new GridLayout(2,2,5,5));
        status.setBorder(BorderFactory.createTitledBorder("Status"));
        p1HpLabel = new JLabel(); p2HpLabel = new JLabel();
        p1GemsLabel = new JLabel(); p2GemsLabel = new JLabel();
        status.add(p1HpLabel); status.add(p2HpLabel);
        status.add(p1GemsLabel); status.add(p2GemsLabel);

        p1ImgLabel = new JLabel(icons.get("p1_idle"));
        p2ImgLabel = new JLabel(icons.get("p2_idle"));
        top.add(p1ImgLabel, BorderLayout.WEST);
        top.add(status, BorderLayout.CENTER);
        top.add(p2ImgLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Grid
        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE,2,2));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY,2));
        buttons = new JButton[GRID_SIZE][GRID_SIZE];
        refreshGrid(gridPanel);
        add(gridPanel, BorderLayout.CENTER);

        // Controls
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
        ctrl.setBorder(BorderFactory.createTitledBorder("Controls"));
        wordLabel = new JLabel("Current: ");
        JButton submit = new JButton("Submit");
        JButton clear = new JButton("Clear");
        JButton shop = new JButton("Shop");
        roundLabel = new JLabel("Round: " + roundCount);

        submit.addActionListener(e -> onSubmit(gridPanel));
        clear.addActionListener(e -> clearSelection());
        shop.addActionListener(e -> openShop(gridPanel));
        ctrl.add(wordLabel); ctrl.add(submit);
        ctrl.add(clear); ctrl.add(shop);
        ctrl.add(roundLabel);
        add(ctrl, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820,840);
        setLocationRelativeTo(null);
        setVisible(true);
        updateStatus();
    }

    private void refreshGrid(JPanel panel) {
        panel.removeAll();
        for (int r=0; r<GRID_SIZE; r++) {
            for (int c=0; c<GRID_SIZE; c++) {
                Tile t = gridModel.getTile(r,c);
                String html = String.format(
                    "<html><center>%c%s<br><font color='black'>%d</font> / <font color='purple'>%d</font></center></html>",
                    t.letter, t.isSpecial()?"<font color='red'>*</font>":"", t.getDmgPts(), t.getGemPts()
                );
                JButton b = new JButton(html);
                if (t.isSpecial()) b.setBackground(Color.PINK);
                b.setFont(new Font("Monospaced", Font.PLAIN, 16));
                b.putClientProperty("pos", new Position(r,c));
                b.addActionListener(e -> onLetterClick((JButton)e.getSource()));
                buttons[r][c] = b;
                panel.add(b);
            }
        }
        panel.revalidate(); panel.repaint();
    }

    private void onLetterClick(JButton btn) {
        playSound("select.wav");
        Position p = (Position)btn.getClientProperty("pos");
        if (selectedPos.contains(p)) {
            int idx = selectedPos.indexOf(p);
            if (idx == selectedPos.size() - 1) {
                JButton deselectedBtn = selectedBtn.get(idx);
                Position rem = selectedPos.get(idx);
                // คืนสีเดิม: ถ้าเป็น special ให้ชมพู ถ้าไม่ใช่ให้ใส่ null
                if (gridModel.getTile(rem.row, rem.col).isSpecial()) {
                    deselectedBtn.setBackground(Color.PINK);
                } else {
                    deselectedBtn.setBackground(null);
                }
                selectedBtn.remove(idx);
                selectedPos.remove(idx);
                updateWordLabel();
            }
            return;
        }
        if (!selectedPos.isEmpty()) {
            Position last = selectedPos.get(selectedPos.size()-1);
            if (Math.abs(p.row - last.row) > 1 || Math.abs(p.col - last.col) > 1) return;
        }
        selectedPos.add(p);
        selectedBtn.add(btn);
        btn.setBackground(Color.YELLOW);
        updateWordLabel();
    }

    private void updateWordLabel() {
        StringBuilder sb = new StringBuilder();
        for (Position pos : selectedPos) sb.append(gridModel.getTile(pos.row,pos.col).letter);
        wordLabel.setText("Current: " + sb.toString().toLowerCase());
    }

    private void onSubmit(JPanel gridPanel) {
        playSound("attack.wav");
        String word = wordLabel.getText().substring(9);
        boolean special = selectedPos.stream().anyMatch(p -> gridModel.getTile(p.row,p.col).isSpecial());
        if (word.length()<3 || !dict.contains(word)) {
            JOptionPane.showMessageDialog(this, "Invalid word");
        } else {
            int dmg=0, gems=0;
            for (Position p : selectedPos) {
                Tile t = gridModel.getTile(p.row,p.col);
                dmg += t.getDmgPts();
                gems += t.getGemPts();
            }
            if (special) {
                dmg *= 2;
                JOptionPane.showMessageDialog(this, "Special tile! Damage doubled and board will reset.");
            }
            // Shield effect
            if (opponent.isShieldActive()) {
                dmg /= 2;
                opponent.setShield(false);
                JOptionPane.showMessageDialog(this, opponent.name + "'s shield blocked half the damage!");
            }
            // Apply damage
            opponent.hp -= dmg;
            // Award gems
            if (currentPlayer == player1) p1Gems += gems; else p2Gems += gems;
            JOptionPane.showMessageDialog(this, currentPlayer.name + " dealt " + dmg + " dmg, +" + gems + " gems");

            // Update grid
            gridModel = special ? new Grid(GRID_SIZE) : gridModel;
            if (!special) gridModel.removeAndCollapse(selectedPos);
            selectedPos.clear(); selectedBtn.clear();
            refreshGrid(gridPanel);

            nextTurn();
        }
        clearSelection(); updateStatus();
    }

    private void clearSelection() {
        for (JButton b : selectedBtn) b.setBackground(null);
        selectedBtn.clear(); selectedPos.clear();
        wordLabel.setText("Current: ");
    }

    private void openShop(JPanel gridPanel) {
        int count = 0;
        while (count < MAX_PURCHASES) {
            int gemsAvail = (currentPlayer==player1? p1Gems : p2Gems);
            int remaining = MAX_PURCHASES - count;
            String[] opts = {"Heal (5)","Shield (3)","BuffAtk (4)","Shuffle (6)","Close"};
            String msg = "Gems: " + gemsAvail + "\nPurchases left: " + remaining + "\nSelect Item to buy";
            int choice = JOptionPane.showOptionDialog(
                this,
                msg,
                currentPlayer.name + " Shop",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null, opts, opts[0]
            );
            if (choice < 0 || choice == 4) break;
            switch (choice) {
                case 0: // Heal
                    if (gemsAvail >= 5) {
                        currentPlayer.hp = Math.min(currentPlayer.maxHp, currentPlayer.hp+20);
                        gemsAvail -= 5;
                        playSound("heal.wav");
                    } else {
                        JOptionPane.showMessageDialog(this, "Not enough Gems");
                    }
                    break;
                case 1: // Shield
                    if (gemsAvail >= 3) {
                        currentPlayer.setShield(true);
                        gemsAvail -= 3;
                        playSound("defend.wav");
                        JOptionPane.showMessageDialog(this, "Shield activated! Next incoming damage will be halved.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Not enough Gems");
                    }
                    break;
                case 2: // BuffAtk
                    if (gemsAvail >= 4) {
                        currentPlayer.buffAttack += 10;
                        gemsAvail -= 4;
                    } else {
                        JOptionPane.showMessageDialog(this, "Not enough Gems");
                    }
                    break;
                case 3: // Shuffle
                    if (gemsAvail >= 6) {
                        gridModel.shuffle();
                        refreshGrid(gridPanel);
                        gemsAvail -= 6;
                        playSound("shuffle.wav");
                    } else {
                        JOptionPane.showMessageDialog(this, "Not enough Gems");
                    }
                    break;
            }
            // Update gem count
            if (currentPlayer == player1) p1Gems = gemsAvail; else p2Gems = gemsAvail;
            count++;
            updateStatus();
        }
        if (count >= MAX_PURCHASES) {
            JOptionPane.showMessageDialog(this, "You have used all your purchases this turn.");
        }
    }

    private void nextTurn() {
        if (opponent.hp <= 0) {
            playSound("ko.wav");
            JOptionPane.showMessageDialog(this, currentPlayer.name + " Wins!");
            playSound("gameover.wav");
            System.exit(0);
        }
        // swap
        Entity tmp = currentPlayer; currentPlayer = opponent; opponent = tmp;
        roundCount++;
        roundLabel.setText("Round: " + roundCount);
        playSound("round" + roundCount + ".wav");
    }

    private void updateStatus() {
        p1HpLabel.setText(player1.name + " HP: " + player1.hp + "/" + player1.maxHp);
        p2HpLabel.setText(player2.name + " HP: " + player2.hp + "/" + player2.maxHp);
        p1GemsLabel.setText("Gems: " + p1Gems);
        p2GemsLabel.setText("Gems: " + p2Gems);
        roundLabel.setText("Round: " + roundCount);
    }

    private void playSound(String fn) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(getClass().getResource("/sounds/" + fn))) {
            Clip clip = AudioSystem.getClip(); clip.open(in); clip.start();
        } catch (Exception e) {
            if (DEV_MODE) e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String[] modes = {"1 Player","2 Player"};
        int m = JOptionPane.showOptionDialog(
            null,
            "Select Game Mode",
            "Bookworm Puzzle RPG",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null, modes, modes[0]
        );
        if (m == 1) SwingUtilities.invokeLater(Bookworm2Player::new);
        else SwingUtilities.invokeLater(BookwormUI::new);
    }
}
