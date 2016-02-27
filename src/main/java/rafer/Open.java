package rafer;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Open {
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode dir;
        FileNode lst;
        FileNode storeRoot;
        FileNode storeFile;
        Date date;
        Launcher iridient;

        world = new World();
        dir = world.guessProjectHome(Open.class);
        lst = dir.join("test.txt");
        lst.deleteFileOpt();
        dir.exec("osascript", "dump");
        storeRoot = (FileNode) world.getHome().join("Pictures/Rafer");
        iridient = dir.launcher("open", "-a", "Iridient Developer");
        for (String name : lst.readLines()) {
            date = Main.LINKED_FMT.parse(name.substring(1, 7));
            storeFile = storeRoot.join(Main.FMT.format(date)).join(removeExtension(name) + ".RAF");
            System.out.println(storeFile.getAbsolute());
            storeFile.checkFile();
            iridient.arg(storeFile.getAbsolute());
        }
        iridient.exec();
    }

    private static String removeExtension(String str) {
        int idx;

        idx = str.lastIndexOf('.');
        if (idx == -1) {
            throw new IllegalArgumentException(str);
        }
        return str.substring(0, idx);
    }
}
