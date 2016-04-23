package rafer;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Selected {
    private static final String[] FIND_JPG = { "find", ".", "-name", "r??????x????.JPG" };
    private static final String[] FIND_RAW = { "find", ".", "-name", "r??????x????.RAF" };

    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode jpgRoot;
        List<String> lines;
        FileNode jpg;
        Map<String, FileNode> jpgs;
        String name;
        FileNode rawRoot;
        FileNode raw;
        FileNode dest;

        world = new World();
        rawRoot = (FileNode) world.getHome().join("Pictures/Rafer/2016/04");
        jpgRoot = (FileNode) world.getHome().findOne("Pictures/*.photoslibrary");
        dest = (FileNode) world.getHome().join("Dropbox/Jugendherberge");
        lines = Separator.RAW_LINE.split(jpgRoot.exec(FIND_JPG));
        System.out.println(lines.size());
        jpgs = new HashMap<>();
        for (String line : lines) {
            line = line.trim();
            jpg = jpgRoot.join(line);
            name = removeExtension(jpg.getName());
            jpgs.put(name, jpg);
        }
        lines = Separator.RAW_LINE.split(rawRoot.exec(FIND_RAW));
        for (String line : lines) {
            raw = rawRoot.join(line.trim());
            name = removeExtension(raw.getName());
            jpg = jpgs.get(name);
            if (jpg == null) {
                System.out.println("missing: " + name);
            } else {
                jpg.copyFile(dest.join(jpg.getName()));
            }
        }
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
