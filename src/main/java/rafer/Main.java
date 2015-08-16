package rafer;

import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.MoveException;
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
    public static final String JPG = ".jpg";

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String ... args) {
        World world;
        Console console;
        FileNode card;
        FileNode dest;
        FileNode backup;
        FileNode fotostream;

        world = new World();
        console = Console.create(world);
        if (args.length != 1) {
            console.error.println("unexpected argument(s)");
            return 1;
        }
        card = world.file("/Volumes/UNTITLED");
        dest = (FileNode) console.world.getHome().join("Downloads/card/dng");
        backup = (FileNode) console.world.getHome().join("Downloads/card/backup");
        fotostream = (FileNode) console.world.getHome().join("Downloads/card/jpg");
        try {
            new Main(console, card, dest, backup, fotostream).run();
            return 0;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            console.error.println(e.getMessage());
            e.printStackTrace(console.verbose);
            return 1;
        }
    }

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd");

    //--

    private final Console console;
    private final FileNode card;
    private final FileNode destDng;
    private final FileNode backup;
    private final FileNode fotostream;

    public Main(Console console, FileNode card, FileNode destDng, FileNode backup, FileNode fotostream) throws IOException {
        this.console = console;
        // no card, no fun
        this.card = card;
        this.destDng = destDng;
        this.backup = backup;
        this.fotostream = fotostream;
    }

    public void run() throws IOException {
        FileNode tmp;
        List<Node> srcRafs;
        List<String> names;
        long firstTimestamp;
        long lastTimestamp;
        FileNode dcim;
        long timestamp;

        directory("card", card);
        directory("dest", destDng);
        directory("backup", destDng);
        directory("fotostream", fotostream);
        tmp = console.world.getTemp().createTempDirectory();

        dcim = card.join("DCIM");
        srcRafs = findRafs(dcim);
        names = download(srcRafs, tmp);
        onCardBackup(dcim);
        if (card.getParent().getName().equals("/Volumes")) {
            eject();
        }
        toDng(tmp, names);
        dates(tmp);
        firstTimestamp = Long.MAX_VALUE;
        lastTimestamp = Long.MIN_VALUE;
        for (Node dng : tmp.find("*" + DNG)) {
            timestamp = dng.getLastModified();
            firstTimestamp = Math.min(firstTimestamp, timestamp);
            lastTimestamp = Math.max(lastTimestamp, timestamp);
        }
        console.info.println("images ranging from " + FMT.format(new Date(firstTimestamp)) + " to "
                + FMT.format(new Date(lastTimestamp)));
        geotags(tmp, firstTimestamp);
        copyToDest(tmp, names);
        console.info.println("backup ...");
        backup(destDng, backup);
        console.info.println("foto stream ...");
        fotostream(tmp, names);
        tmp.deleteTree();
    }

    private void fotostream(FileNode tmp, List<String> names) throws Failure, MoveException {
        Launcher launcher;

        launcher = tmp.launcher("osascript", "/Users/mhm/Projects/foss/rafer/as");
        for (String name : names) {
            launcher.arg(tmp.join(name + DNG).getURI().toString());
        }
        console.info.println(launcher);
        launcher.exec(console.info);
        for (String name : names) {
            tmp.join(name + DNG + JPG).move(fotostream.join(name + JPG));
        }
    }

    private void backup(FileNode srcRoot, FileNode destRoot) throws IOException {
        String path;

        for (Node src : srcRoot.find("**/*" + DNG)) {
            path = src.getRelative(srcRoot);
            FileNode dest = destRoot.join(path);
            if (!dest.exists()) {
                dest.getParent().mkdirsOpt();
                console.info.println("A " + destRoot.getName() + "/" + path);
                src.copyFile(dest);
                dest.setLastModified(src.getLastModified());
            }
        }

        for (Node dest : destRoot.find("**/*" + DNG)) {
            path = dest.getRelative(destRoot);
            FileNode src = srcRoot.join(path);
            if (!src.exists()) {
                console.info.println("D " + destRoot.getName() + "/" + path);
                dest.deleteFile();
            }
        }
    }

    private void copyToDest(FileNode srcDir, List<String> names) throws IOException {
        FileNode src;
        FileNode dest;

        for (String name : names) {
            name = name + DNG;
            src = srcDir.join(name);
            dest = destDng.join(FMT.format(src.getLastModified()), name);
            dest.getParent().mkdirsOpt();
            dest.checkNotExists();
            src.copyFile(dest);
            dest.setLastModified(src.getLastModified());
        }
    }

    private List<Node> findRafs(FileNode dcim) throws IOException {
        List<Node> result;
        String name;

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
                name = other.getName();
                if (name.endsWith(JPG) && result.contains(other.getParent().join(Strings.removeRight(name, JPG) + RAF))) {
                    console.info.println("ignoring sidecar jpg: " + other);
                } else {
                    throw new IOException("unexpected file in image folder: " + other);
                }
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
    private List<String> download(List<Node> srcRafs, FileNode dest) throws IOException {
        List<String> result;
        String name;
        FileNode destRaf;

        console.info.println("downloading " + srcRafs.size() + " images to " + dest);
        result = new ArrayList<>(srcRafs.size());
        for (Node srcRaf : srcRafs) {
            destRaf = dest.join(srcRaf.getName());
            name = Strings.removeRight(destRaf.getName(), RAF);
            if (result.contains(name)) {
                throw new IOException("naming clash: " + name);
            }
            result.add(name);
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

    private void geotags(FileNode dir, long firstTimestamp) throws IOException {
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
        launcher.arg("-P", "-api", "GeoMaxIntSecs=43200"); // 12 Stunden, weil er keine neuen Punkte speichert, wenn man sich nicht bewegt
        for (FileNode track : tracks) {
            launcher.arg("-geotag");
            launcher.arg(track.getAbsolute());
        }
        launcher.arg("-ext", DNG);
        launcher.arg(".");
        if (tracks.isEmpty()) {
            console.info.println("no matching gpx files");
        } else {
            console.info.println(launcher.toString());
            launcher.exec(console.info);
        }
    }

    private void dates(FileNode dir) throws IOException {
        Launcher launcher;

        launcher = new Launcher(dir, "exiftool", "-FileModifyDate<DateTimeOriginal", ".", "-ext", DNG);
        launcher.exec(console.info);
    }
}
