import game.GameManager;

import java.util.Scanner;

// Hlavni smycka hry: cte prikazy z konzole a posila je do GameManageru.
void main() {
    GameManager game = new GameManager();
    Scanner scanner = new Scanner(System.in);

    System.out.println("=== FARMA NA KONCI SVĚTA ===");
    System.out.println(game.processCommand("stav"));

    while (!game.isGameOver()) {
        System.out.print("\n> ");
        if (!scanner.hasNextLine()) break;
        String line = scanner.nextLine();
        if (line.trim().equalsIgnoreCase("konec")) break;
        System.out.println(game.processCommand(line));
    }
    System.out.println("Konec hry.");
}