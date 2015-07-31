package rafer;

import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static final String RAF = ".RAF";
    public static final String DNG = ".dng";

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String ... args) {
        World world;
        Console console;
        FileNode card;
        FileNode dest;
        FileNode tmp;

        world = new World();
        console = Console.create(world);
        if (args.length != 1) {
            console.error.println("unexpected argument(s)");
            return 1;
        }
        card = world.file("/Volumes/UNTITLED");
        tmp = (FileNode) console.world.getHome().join("Downloads/card/raf");
        dest = (FileNode) console.world.getHome().join("Downloads/card/dng");
        try {
            new Main(console, card, tmp, dest).run();
            return 0;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            console.error.println(e.getMessage());
            e.printStackTrace(console.verbose);
            return 1;
        }
    }

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
    private static final SimpleDateFormat PATH_FMT = new SimpleDateFormat("yyyy-MM-dd");

    //--

    private final Console console;
    private final FileNode card;
    private final FileNode destDng;
    private final FileNode tmpDir;

    public Main(Console console, FileNode card, FileNode tmpDir, FileNode destDng) throws IOException {
        this.console = console;
        // no card, no fun
        this.card = card;
        this.tmpDir = tmpDir;
        this.destDng = destDng;
    }

    public void run() throws IOException {
        FileNode tmp;
        List<Node> srcRafs;
        Map<String, Long> names;
        long firstTimestamp;
        long lastTimestamp;
        FileNode dcim;

        directory("card", card);
        directory("dest", destDng);
        tmp = tmpDir.join(FMT.format(new Date()));
        tmp.mkdir();

        dcim = card.join("DCIM");
        srcRafs = findRafs(dcim);
        names = download(srcRafs, tmp);
        firstTimestamp = Long.MAX_VALUE;
        lastTimestamp = Long.MIN_VALUE;
        for (Long timestamp : names.values()) {
            firstTimestamp = Math.min(firstTimestamp, timestamp);
            lastTimestamp = Math.max(lastTimestamp, timestamp);
        }
        console.info.println("done, images range from " + FMT.format(new Date(firstTimestamp)) + " to "
                + FMT.format(new Date(lastTimestamp)));
        onCardBackup(dcim);
        if (card.getParent().getName().equals("/Volumes")) {
            eject();
        }
        toDng(tmp, names.keySet());
        geotags(tmp, names.keySet(), firstTimestamp);
        copy(tmp, names);
    }

    private void copy(FileNode srcDir, Map<String, Long> names) throws IOException {
        String name;
        FileNode src;
        FileNode dest;

        for (Map.Entry<String, Long> entry : names.entrySet()) {
            name = entry.getKey() + DNG;
            src = srcDir.join(name);
            dest = destDng.join(PATH_FMT.format(new Date(entry.getValue())), name);
            dest.getParent().mkdirsOpt();
            dest.checkNotExists();
            src.copyFile(dest);
            dest.setLastModified(entry.getValue());
        }
    }

    private static List<Node> findRafs(FileNode dcim) throws IOException {
        List<Node> result;

        dcim.checkDirectory();
        result = dcim.find("**/*.RAF");
        if (result.isEmpty()) {
            throw new IOException("no images in " + dcim);
        }
        for (Node other : dcim.find("**/*")) {
            if (other.isDirectory()) {
                // ignore
            } else if (other.getName().equals(".DS_Store")) {
                // ignore
            } else if (!result.contains(other)) {
                throw new IOException("unexpected file in image folder: " + other);
            }
        }
        return result;
    }

    private static void directory(String name, FileNode dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException(name + " not found: " + dir.getAbsolute());
        }
    }

    /** @return names -> lastModified */
    private Map<String, Long> download(List<Node> srcRafs, FileNode dest) throws IOException {
        Map<String, Long> result;
        String name;
        FileNode destRaf;

        console.info.println("downloading " + srcRafs.size() + " images to " + dest);
        result = new HashMap<>(srcRafs.size());
        for (Node srcRaf : srcRafs) {
            destRaf = dest.join(srcRaf.getName());
            name = Strings.removeRight(destRaf.getName(), RAF);
            if (result.put(name, srcRaf.getLastModified()) != null) {
                throw new IOException("naming clash: " + name);
            }
            srcRaf.copyFile(destRaf);
        }
        return result;
    }

    private void toDng(FileNode working, Collection<String> names) throws Failure {
        long millis;
        Launcher converter;

        console.info.println("converting " + names.size() + " images");
        millis = System.currentTimeMillis();
        converter = new Launcher(working, "/Applications/Adobe DNG Converter.app/Contents/MacOS/Adobe DNG Converter");
        for (String name : names) {
            converter.arg(name + RAF);
        }
        converter.exec(console.info);
        millis = System.currentTimeMillis() - millis;
        millis = (millis + 500) / 1000;
        console.info.println("(" + millis + "s, " + millis / names.size() + "s/pic)");
    }

    private void eject() {
        try {
            console.info.println(card.getParent().exec("diskutil", "eject", card.getName()));
        } catch (IOException e) {
            console.info.println("WARNING: eject failed");
        }
    }

    private void onCardBackup(FileNode src) throws IOException {
        FileNode downloaded;

        downloaded = card.join("DOWNLOADED");
        if (downloaded.isDirectory()) {
            console.error.println("deleting " + downloaded);
            downloaded.deleteTree();
        }
        downloaded.checkNotExists();

        console.info.println("moving " + src + " to " + downloaded);
        src.move(downloaded);
    }

    private void geotags(FileNode dir, Collection<String> names, long firstTimestamp) throws IOException {
        List<FileNode> tracks;
        Launcher launcher;

        tracks = new ArrayList<>();
        for (Node track : console.world.getHome().join("Dropbox/Apps/Geotag Photos Pro (iOS)").list()) {
            if (track.getName().endsWith(".gpx")) {
                if (track.getLastModified() > firstTimestamp) {
                    tracks.add((FileNode) track);
                }
            }
        }
        launcher = new Launcher(dir, "exiftool", "-overwrite_original");
        launcher.arg("-api", "GeoMaxIntSecs=43200"); // 12 Stunden, weil er keine neuen Punkte speichert, wenn man sich nicht bewegt
        for (FileNode track : tracks) {
            launcher.arg("-geotag");
            launcher.arg(track.getAbsolute());
        }
        for (String name : names) {
            launcher.arg(name + DNG);
        }
        if (tracks.isEmpty()) {
            console.info.println("no matching gpx files");
        } else {
            console.info.println(launcher.toString());
            launcher.exec(console.info);
        }
    }
}
