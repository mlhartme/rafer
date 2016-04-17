package rafer;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Scratch {
    private static final SimpleDateFormat OLD = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat NEW = new SimpleDateFormat("yyyy/MM/dd");
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode root;
        Date date;
        int count;
        FileNode renamed;
        FileNode dest;

        world = new World();
        root = world.file("/Volumes/Data/Lightroom");
        root.checkDirectory();
        renamed = world.file("/Volumes/Data/Rafer");
        renamed.checkDirectory();
        count = 0;
        for (Node file : root.find("*/*/*")) {
            if (file.isDirectory()) {
                throw new IOException("not a file: " + file);
            }
            date = OLD.parse(file.getParent().getName());
            dest = renamed.join(NEW.format(date), file.getName());
            file.move(dest);
            count++;
            System.out.println(dest);
        }
        System.out.println();
        System.out.println("moved " + count + " files");
    }
}
