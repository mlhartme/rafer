package rafer;


import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Test;

import java.io.IOException;

public class MainTest {
    @Test
    public void foo() throws IOException {
        World world;
        FileNode home;
        FileNode root;
        Main main;
        FileNode card;

        world = new World();
        home = world.guessProjectHome(MainTest.class);
        root = home.join("target/it");
        root.deleteTreeOpt();
        root.mkdirsOpt();
        card = root.join("card").mkdir();
        home.join("src/test/rafs").copyDirectory(card.join("DCIM").mkdir());
        main = new Main(Console.create(world), card, root.join("tmp").mkdir(), root.join("dest").mkdir());
        main.run();
    }
}
