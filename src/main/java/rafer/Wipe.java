package rafer;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Wipe {
    private static final String[] FIND = { "find", ".", "-name", "r??????x????.JPG" };
    private static final String[] FINDRAW = { "find", ".", "-name", "r??????x????.RAF" };

    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode mediathek;
        List<String> lines;
        FileNode jpg;
        String name;
        FileNode storeRoot;
        Date date;
        FileNode storeFile;
        List<String> storeNames;
        List<FileNode> remove;
        FileNode raw;

        world = new World();
        storeRoot = world.file("/Volumes/Data/Pictures");
        mediathek = (FileNode) world.getHome().findOne("Pictures/*.photoslibrary");
        lines = Separator.RAW_LINE.split(mediathek.exec(FIND));
        System.out.println(lines.size());
        storeNames = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            jpg = mediathek.join(line);
            name = jpg.getName();
            date = Main.LINKED_FMT.parse(name.substring(1, 7));
            storeFile = storeRoot.join(Main.FMT.format(date)).join(removeExtension(jpg.getName()) + ".RAF");
            if (storeFile.exists()) {
                storeNames.add(removeExtension(storeFile.getName()));
            } else {
                System.out.println("missing: " + storeFile);
            }
        }
        lines = Separator.RAW_LINE.split(storeRoot.exec(FINDRAW));
        remove = new ArrayList<>();
        for (String line : lines) {
            raw = storeRoot.join(line.trim());
            if (!storeNames.contains(removeExtension(raw.getName()))) {
                remove.add(raw);
                System.out.println("remove " + raw);
                raw.deleteFile();
            }
        }
        System.out.println("remove " + remove.size() + "/" + lines.size());
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
