package rafer;

import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException {
          switch (args.length) {
            case 0:
                new Main().run();
                break;
            default:
                throw new IllegalArgumentException("unexpected arguments: " + args.length);
        }
    }

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd-HH:mm");

    //--

    private final World world;
    private final Console console;

    public Main() {
        this.world = new World();
        this.console = Console.create(world);
    }

    public void run() throws IOException {
        FileNode card;
        FileNode src;
        FileNode destRaf;
        FileNode destDng;
        List<Node> list;
        List<String> rafNames;
        long firstTimestamp;

        // no card, no fun
        card = world.file("/Volumes/UNTITLED");
        card.checkDirectory();

        destRaf = (FileNode) world.getHome().join("Downloads/card/raf/" + FMT.format(new Date()));
        destRaf.mkdirs();
        destDng = (FileNode) world.getHome().join("Downloads/card/dng");
        if (!destDng.exists()) {
            destDng.mkdirs();
        } else if (!destDng.find("*.dng").isEmpty()) {
            throw new IOException("previous download has not been imported into Lightroom");
        }

        src = card.join("DCIM");
        src.checkDirectory();
        list = src.find("**/*.RAF");
        if (list.isEmpty()) {
            throw new IOException("no images in " + src);
        }
        for (Node n : src.find("**/*")) {
            if (n.isDirectory()) {
                // ignore
            } else if (!list.contains(n)) {
                throw new IOException("unexpected file in image folder: " + n);
            }
        }

        rafNames = new ArrayList<>();
        firstTimestamp = download(destRaf, list, rafNames);

        onCardBackup(card, src);
        eject(card);
        toDng(destRaf, destDng, rafNames);
        geotags(destDng, rafNames, firstTimestamp);

        console.info.println("please import " + destDng + " into Lightroom (with 'card download' settings)");
        console.info.println("and run bildersync to save some memory");
    }

    private long download(FileNode destRaf, List<Node> list, List<String> rafNames) throws IOException {
        long firstTimestamp;
        long lastTimestamp;
        String name;
        FileNode destImage;
        long timestamp;

        console.info.println("downloading " + list.size() + " images to " + destRaf);
        firstTimestamp = Long.MAX_VALUE;
        lastTimestamp = Long.MIN_VALUE;
        for (Node image : list) {
            name = image.getName();
            destImage = destRaf.join(name);
            image.copyFile(destImage);
            timestamp = image.getLastModified();
            firstTimestamp = Math.min(firstTimestamp, timestamp);
            lastTimestamp = Math.max(lastTimestamp, timestamp);
            destImage.setLastModified(timestamp);
            if (!rafNames.add(name)) {
                throw new IOException("naming clash: " + name);
            }
        }
        console.info.println("done, images range from " + FMT.format(new Date(firstTimestamp)) + " to "
                + FMT.format(new Date(lastTimestamp)));
        return firstTimestamp;
    }

    private void toDng(FileNode destRaf, FileNode destDng, List<String> rafNames) throws Failure {
        long millis;
        Launcher converter;

        console.info.println("converting " + rafNames.size() + " images");
        millis = System.currentTimeMillis();
        converter = new Launcher(destRaf, "/Applications/Adobe DNG Converter.app/Contents/MacOS/Adobe DNG Converter");
        converter.arg("-d", destDng.getAbsolute());
        converter.args(rafNames);
        converter.exec(console.info);
        millis = System.currentTimeMillis() - millis;
        millis = (millis + 500) / 1000;
        console.info.println("(" + millis + "s, " + millis/rafNames.size() + "s/pic)");
    }

    private void eject(FileNode card) {
        try {
            console.info.println(card.getParent().exec("diskutil", "eject", card.getName()));
        } catch (IOException e) {
            console.info.println("WARNING: eject failed");
        }
    }

    private void onCardBackup(FileNode card, FileNode src) throws IOException {
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
