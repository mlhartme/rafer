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
import java.util.Date;
import java.util.List;

public class Main {
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
        List<String> rafNames;
        long firstTimestamp;
        FileNode dcim;

        directory("card", card);
        directory("dest", destDng);
        tmp = tmpDir.join(FMT.format(new Date()));
        tmp.mkdir();

        dcim = card.join("DCIM");
        srcRafs = findRafs(dcim);
        rafNames = new ArrayList<>();
        firstTimestamp = download(srcRafs, tmp, rafNames);
        onCardBackup(dcim);
        if (card.getParent().getName().equals("/Volumes")) {
            eject();
        }
        toDng(tmp, rafNames);
        geotags(destDng, rafNames, firstTimestamp);

        console.info.println("please import " + destDng + " into Lightroom (with 'card download' settings)");
        console.info.println("and run bildersync to save some memory");
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

    private long download(List<Node> srcRafs, FileNode dest, List<String> rafNames) throws IOException {
        long firstTimestamp;
        long lastTimestamp;
        String name;
        FileNode destRaf;
        long timestamp;

        console.info.println("downloading " + srcRafs.size() + " images to " + dest);
        firstTimestamp = Long.MAX_VALUE;
        lastTimestamp = Long.MIN_VALUE;
        for (Node srcRaf : srcRafs) {
            name = srcRaf.getName();
            if (!rafNames.add(name)) {
                throw new IOException("naming clash: " + name);
            }
            destRaf = dest.join(name);
            srcRaf.copyFile(destRaf);
            timestamp = srcRaf.getLastModified();
            firstTimestamp = Math.min(firstTimestamp, timestamp);
            lastTimestamp = Math.max(lastTimestamp, timestamp);
            destRaf.setLastModified(timestamp);
        }
        console.info.println("done, images range from " + FMT.format(new Date(firstTimestamp)) + " to "
                + FMT.format(new Date(lastTimestamp)));
        return firstTimestamp;
    }

    private void toDng(FileNode working, List<String> rafNames) throws Failure {
        long millis;
        Launcher converter;

        console.info.println("converting " + rafNames.size() + " images");
        millis = System.currentTimeMillis();
        converter = new Launcher(working, "/Applications/Adobe DNG Converter.app/Contents/MacOS/Adobe DNG Converter");
        converter.arg("-d", destDng.getAbsolute());
        converter.args(rafNames);
        converter.exec(console.info);
        millis = System.currentTimeMillis() - millis;
        millis = (millis + 500) / 1000;
        console.info.println("(" + millis + "s, " + millis/rafNames.size() + "s/pic)");
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

    private void geotags(FileNode dir, List<String> rafs, long firstTimestamp) throws IOException {
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
        if (tracks.isEmpty()) {
            console.info.println("no matching gpx files");
            return;
        }
        launcher = new Launcher(dir, "exiftool", "-overwrite_original");
        launcher.arg("-api", "GeoMaxIntSecs=43200"); // 12 Stunden, weil er keine neuen Punkte speichert, wenn man sich nicht bewegt
        for (FileNode track : tracks) {
            launcher.arg("-geotag");
            launcher.arg(track.getAbsolute());
        }
        for (String raf : rafs) {
            launcher.arg(Strings.removeRight(raf, ".RAF") + ".dng");
        }
        console.info.println(launcher.toString());
        launcher.exec(console.info);
    }
}
