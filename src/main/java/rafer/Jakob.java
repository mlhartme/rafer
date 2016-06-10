package rafer;

import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Jakob {
    public static final String ANSI_ERASE_END = "\u001B[0J";
    public static final String ANSI_ERASE_ALL = "\u001B[2J";
    public static final String ANSI_DOWN = "\u001B[1B";
    public static final String ANSI_HOME = "\u001B[1;1H";

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static class Aufgabe {
        public final int x;
        public final int y;

        public Aufgabe(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    public static void main(String[] args) {
        Console console;
        Random random;
        String str;
        int ergebnis;
        List<Aufgabe> alles;
        Aufgabe aufgabe;
        int i;
        int count;
        int richtig;
        StringBuilder done;
        List<String> history;
        long start;
        int end = 20;

        history = new ArrayList<>();
        done = new StringBuilder();
        console = Console.create(new World());
        alles = ausdenken();
        console.info.println(ANSI_ERASE_ALL + ANSI_HOME + "Hallo Jakob, laß uns rechnen!");
        random = new Random();
        count = alles.size();
        richtig = 0;
        start = System.currentTimeMillis();
        while (true) {
            console.info.println(done + dots(end - richtig));
            for (String h : history) {
                console.info.println(h);
            }
            if (richtig == end) {
                break;
            }
            i = Math.abs(random.nextInt()) % alles.size();
            aufgabe = alles.get(i);
            str = console.readline(aufgabe.x + " * " + aufgabe.y + " = ").trim();
            try {
                ergebnis = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                ergebnis = 0;
            }
            if (ergebnis == aufgabe.x * aufgabe.y) {
                alles.remove(i);
                richtig++;
                done.append(ANSI_GREEN + "+" + ANSI_RESET);
                history.add(aufgabe.x + " * " + aufgabe.y + " = " + ANSI_GREEN + ergebnis + ANSI_RESET);
            } else {
                done.append(ANSI_RED + "-" + ANSI_RESET);
                history.add(aufgabe.x + " * " + aufgabe.y + " = " + ANSI_RED + str + ANSI_RESET);
            }
            if (history.size() > 15) {
                history.remove(0);
            }
            console.info.print(ANSI_HOME + ANSI_DOWN + ANSI_ERASE_END);
        }
        console.info.println();
        console.info.println("Du hast " + richtig + " richtige Aufgaben in "
                + ((System.currentTimeMillis() - start) / 1000) + " Sekunden gerechnet :)");
    }

    private static String dots(int count) {
        return Strings.times('_', count);
    }

    private static List<Aufgabe> ausdenken() {
        List<Aufgabe> result;

        result = new ArrayList<>();
        for (int x = 2; x <= 9; x++) {
            for (int y = x + 1; y <= 9; y++) {
                result.add(new Aufgabe(x, y));
            }
        }
        return result;
    }
}
