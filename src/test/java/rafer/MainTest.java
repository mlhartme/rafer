package rafer;


import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MainTest {
    @Test
    public void foo() throws IOException {
        World world;
        FileNode home;
        FileNode root;
        Main main;
        FileNode card;
        FileNode dest;
        FileNode rafs;
        FileNode backup;
        FileNode fotostream;
        FileNode gpxTracks;

        world = new World();
        home = world.guessProjectHome(MainTest.class);
        root = home.join("target/it");
        root.deleteTreeOpt();
        root.mkdirsOpt();
        card = root.join("card").mkdir();
        dest = root.join("dest");
        rafs = home.join("src/test/rafs");
        rafs.copyDirectory(card.join("DCIM").mkdir());
        for (Node dng : card.find("**/*" + Main.RAF)) {
            dng.getParent().join(Strings.removeRight(dng.getName(), Main.RAF) + ".jpg").mkfile();
        }
        backup = root.join("backup").mkdir();
        fotostream = root.join("fotostream").mkdir();
        gpxTracks = root.join("gpxTracks").mkdir();
        backup.join("foo.dng").mkfile();
        main = new Main(Console.create(world), card, dest.mkdir(), backup, fotostream, gpxTracks);
        main.run();
        assertEquals(rafs.list().size(), dest.find("**/*.dng").size());
    }
}
