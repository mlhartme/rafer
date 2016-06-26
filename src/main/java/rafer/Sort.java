package rafer;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public class Sort {
    public static void main(String[] args) throws IOException {
        World world;
        FileNode root;
        FileNode waste;
        String name;
        int idx;
        Date date;
        int id;
        FileNode origDir;
        FileNode origFile;

        world = new World();
        origDir = world.file("/Volumes/Elements3T/Bilder");
        origDir.checkDirectory();
        root = (FileNode) world.getHome().join("Pictures/Sync");
        waste = root.getParent().join("waste");
        int count = 0;
        for (Node file : root.find("**/*.JPG")) {
            name = file.getName();
            name = Strings.removeRight(name, ".JPG");
            idx = name.indexOf('x');
            if (idx == -1) {
                throw new IllegalStateException();
            }
            try {
                date = Main.LINKED_FMT.parse(name.substring(1, idx));
            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
            id = Integer.parseInt(name.substring(idx + 1));
            if (date.getYear() == 115) {
                origFile = origDir.join(Main.FMT.format(date), name + Main.RAF);
                if (!origFile.exists()) {
                    count++;
                    System.out.println("match " + name);
                    file.move(waste.join(file.getName()));
                }
            }
        }
        System.out.println("count: " + count);
    }
}
