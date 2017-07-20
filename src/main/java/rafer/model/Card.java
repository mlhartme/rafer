package rafer.model;

import net.mlhartme.smuggler.cache.FolderData;
import net.mlhartme.smuggler.smugmug.Account;
import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;
import rafer.Sync;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Card {
    private final FileNode directory;

    public Card(FileNode directory) {
        this.directory = directory;
    }

    public boolean available() {
        return directory.isDirectory();
    }

    /** @return list of raf files in dest */
    public List<FileNode> download(Console console, FileNode dest) throws IOException {
        List<FileNode> cardRafs;
        List<FileNode> downloaded;
        FileNode dcim;

        Sync.directory("card", directory);

        dcim = directory.join("DCIM");
        cardRafs = Sync.findRafs(console, dcim);
        downloaded = download(console, cardRafs, dest);
        onCardBackup(console, dcim, downloaded);
        ejectOpt(console);
        return downloaded;
    }

    public void process(FileNode images, Console console, Account smugmugAccount, FolderData smugmugRoot, FileNode gpxTracks, FileNode rafs, FileNode smugmug) throws IOException {
        Map<String, Long> pairs;
        Collection<Long> values;
        long firstTimestamp;
        long lastTimestamp;

        Sync.directory("gpxTracks", gpxTracks);
        pairs = timestamps(console, images);
        values = pairs.values();
        firstTimestamp = Collections.min(values);
        lastTimestamp = Collections.max(values);
        console.info.println("images ranging from " + Utils.DAY_FMT.format(new Date(firstTimestamp)) + " to " + Utils.DAY_FMT.format(new Date(lastTimestamp)));
        geotags(console, gpxTracks, images, firstTimestamp);
        console.info.println("saving rafs at " + rafs + " ...");
        moveRafs(images, pairs, rafs);
        if (smugmug != null) {
            console.info.println("smugmug upload ...");
            uploadJpegs(smugmugAccount, smugmugRoot, images, pairs.keySet());
        }
        images.deleteDirectory(); // it's empty now
    }

    private Map<String, Long> timestamps(Console console, FileNode tmp) throws IOException {
        Map<String, Long> result;
        long timestamp;
        String origName;
        String linkedName;

        dates(console, tmp);
        result = new HashMap<>();
        for (Node raf : tmp.find("*" + Utils.RAF)) {
            timestamp = raf.getLastModified();
            origName = Strings.removeRight(raf.getName(), Utils.RAF);
            linkedName = linked(origName, timestamp);
            result.put(linkedName, timestamp);
            raf.move(tmp.join(linkedName + Utils.RAF));
            tmp.join(origName + Utils.JPG).move(tmp.join(linkedName + Utils.JPG));
        }
        return result;
    }

    private void dates(Console console, FileNode dir) throws IOException {
        Launcher launcher;

        launcher = new Launcher(dir, "exiftool", "-FileModifyDate<DateTimeOriginal", ".", "-ext", Utils.RAF);
        launcher.exec(console.info);
    }

    private String linked(String pair, long timestamp) {
        String id;

        id = Strings.removeLeft(pair, "DSCF");
        return "r" + Utils.LINKED_FMT.format(new Date(timestamp)) + "x" + id;
    }

    private void uploadJpegs(Account account, FolderData root, FileNode tmp, Collection<String> names) throws IOException {
        FileNode dest;

        for (String name : names) {
            dest = Sync.getJpgFile(name, tmp);
            root.getOrCreateAlbum(account, dest.getRelative(tmp)).upload(account, tmp.join(name));
        }
    }

    private void geotags(Console console, FileNode gpxTracks, FileNode dir, long firstTimestamp) throws IOException {
        List<FileNode> tracks;
        Launcher launcher;

        tracks = new ArrayList<>();
        for (Node track : gpxTracks.list()) {
            if (track.getName().endsWith(".gpx")) {
                if (track.getLastModified() > firstTimestamp) {
                    tracks.add((FileNode) track);
                }
            }
        }
        launcher = new Launcher(dir, "exiftool", "-overwrite_original");
        launcher.arg("-Artist=Michael Hartmeier", "-Copyright=All rights reserved");
        launcher.arg("-P", "-api", "GeoMaxIntSecs=43200"); // 12 Stunden, weil er keine neuen Punkte speichert, wenn man sich nicht bewegt
        for (FileNode track : tracks) {
            launcher.arg("-geotag");
            launcher.arg(track.getAbsolute());
        }
        launcher.arg("-ext", Utils.RAF);
        launcher.arg("-ext", Utils.JPG);
        launcher.arg(".");
        if (tracks.isEmpty()) {
            console.info.println("no matching gpx files");
        } else {
            console.info.println(launcher.toString());
            launcher.exec(console.info);
        }
    }

    private void moveRafs(FileNode srcDir, Map<String, Long> pairs, FileNode destDir) throws IOException {
        FileNode src;
        FileNode dest;

        for (Map.Entry<String, Long> entry : pairs.entrySet()) {
            src = srcDir.join(entry.getKey() + Utils.RAF);
            dest = destDir.join(Utils.MONTH_FMT.format(entry.getValue()), src.getName());
            dest.getParent().mkdirsOpt();
            dest.checkNotExists();
            src.move(dest); // dont copy - disk might be full
            dest.setLastModified(entry.getValue());
        }
    }

    /** @return srcRafs actually downloaded */
    private List<FileNode> download(Console console, List<FileNode> srcRafs, FileNode dest) throws IOException {
        List<FileNode> result;
        FileNode destRaf;
        FileNode destJpg;
        FileStore store;
        long available;

        store = Files.getFileStore(dest.toPath());
        console.info.println("downloading " + srcRafs.size() + " images to " + dest);
        result = new ArrayList<>(srcRafs.size());
        for (FileNode srcRaf : srcRafs) {
            available = store.getUsableSpace();
            if (available < 1024l * 1024 * 1024) {
                System.out.println("WARNING: disk space is low -- download is incomplete!");
                break;
            }
            destRaf = dest.join(srcRaf.getName());
            destJpg = Sync.jpg(destRaf);
            result.add(srcRaf);
            srcRaf.copyFile(destRaf);
            Sync.jpg(srcRaf).copyFile(destJpg);
        }
        return result;
    }

    public void ejectOpt(Console console) {
        if (directory.getParent().getName().equals("Volumes")) {
            eject(console);
        }
    }

    public void eject(Console console) {
        try {
            console.info.println(directory.exec("diskutil", "eject", directory.getName()));
        } catch (IOException e) {
            e.printStackTrace(console.verbose);
            console.info.println("WARNING: eject failed");
        }
    }

    private void onCardBackup(Console console, FileNode src, List<FileNode> srcRafs) throws IOException {
        FileNode downloaded;
        String path;
        FileNode destRaf;

        downloaded = directory.join("DOWNLOADED");
        if (downloaded.isDirectory()) {
            console.error.println("deleting " + downloaded);
            downloaded.deleteTree();
        }
        downloaded.checkNotExists();

        console.info.println("moving " + srcRafs.size() + " images from " + src + " to " + downloaded);
        for (FileNode srcRaf : srcRafs) {
            path = srcRaf.getRelative(src);
            destRaf = downloaded.join(path);
            destRaf.getParent().mkdirsOpt();
            srcRaf.move(destRaf);
            Sync.jpg(srcRaf).move(Sync.jpg(destRaf));
        }
        if (Sync.findRafs(console, src).isEmpty()) {
            console.info.println("complete download, removing " + src);
        }
    }

}
