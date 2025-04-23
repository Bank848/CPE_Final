/*
 * GameLauncher.java
 * Entry point to select between single-player and two-player modes.
 */
import javax.swing.*;

public class GameLauncher {
    public static void main(String[] args) {
        String[] modes = {"1 Player", "2 Player"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "Select Game Mode",
            "Bookworm Puzzle RPG - Launcher",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            modes,
            modes[0]
        );
        if (choice == 1) {
            SwingUtilities.invokeLater(Bookworm2Player::new);
        } else {
            SwingUtilities.invokeLater(BookwormUI::new);
        }
    }
}
