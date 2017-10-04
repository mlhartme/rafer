package rafer;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class Scratch {
    public static void main(String[] args) throws IOException {
        copyDate();
    }

    public static void copyDate() throws IOException {
        World world;
        FileNode dest;
        String date;
        String time;

        world = World.create();
        dest = world.getHome().join("Desktop/einsortieren/dateCreated");
        for (FileNode file : dest.list()) {
            if (!file.isFile() || file.getName().startsWith(".")) {
                continue;
            }
            date = dest.exec("exiftool", "-j", "-DateCreated", "-TimeCreated", "-DateTimeOriginal", file.getName()).trim();
            if (date.isEmpty()) {
                throw new IOException(file + ": missing date");
            }
            time = dest.exec("exiftool", "-TimeCreated", file.getName()).trim();
            if (time.isEmpty()) {
                throw new IOException(file + ": missing time");
            }
            System.out.println(file.getName() + ": " + date + " " + time);
        }
    }


    public static void stripDatePart() throws IOException {
        World world;
        FileNode dest;
        String name;

        world = World.create();
        dest = world.getHome().join("Desktop/einsortieren/dateCreated");
        for (FileNode file : dest.list()) {
            if (!file.isFile()) {
                continue;
            }
            name = file.getName();
            if (!name.startsWith("r") || name.charAt(7) != 'x') {
                throw new IOException("ERROR: " + name);
            }
            file.move(file.getParent().join(name.substring(8)));
        }
    }

    public static void dateCreatedToOneDirectory() throws IOException {
        World world;
        FileNode dest;
        FileNode dir;
        String str;

        world = World.create();
        dest = world.getHome().join("Desktop/einsortieren/dateCreated");
        for (FileNode file : world.getHome().join("Desktop/einsortieren/TODO-created-date").find("**/*")) {
            if (!file.isFile()) {
                continue;
            }
            if (!file.getName().endsWith("JPG")) {
                throw new IOException("ERROR: " + file.getName());
            }
            dir = file.getParent();
            str = dir.exec("exiftool", "-DateCreated", file.getName()).trim();
            if (!str.isEmpty()) {
                System.out.println(file + " " + str);
                file.move(dest.join(file.getName()));
            }
        }
    }
}
