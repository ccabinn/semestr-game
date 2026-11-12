import game.GameManager;
import gui.GameWindow;

import javax.swing.SwingUtilities;

//main
public class Main {
    public static void main(String[] args) {
        GameManager gameManager = new GameManager();

        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(gameManager);
            window.setVisible(true);
        });
    }
}