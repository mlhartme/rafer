/*
 * Copyright Michael Hartmeier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rafer;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static final String RAF = ".RAF";
    public static final String JPG = ".JPG";

    public static void main(String[] args) throws IOException {
        System.exit(run(args));
    }

    public static int run(String ... args) throws IOException {
        World world;
        Console console;
        FileNode card;
        FileNode rafs;
        List<FileNode> backups;
        FileNode gpxTracks;
        FileNode jpegs;
        FileNode trash;

        world = World.create();
        console = Console.create();
        if (args.length != 0) {
            console.error.println("unexpected argument(s)");
            return 1;
        }
        card = world.file("/Volumes/UNTITLED");
        rafs = world.getHome().join("Pictures/Rafer");
        jpegs = world.getHome().join("Timeline");
        backups = new ArrayList<>();
        backups.add(world.file("/Volumes/Data/Bilder"));
        backups.add(world.file("/Volumes/Neuerkeller/Bilder"));
        gpxTracks = world.getHome().join("Dropbox/Apps/Geotag Photos Pro (iOS)");
        trash = world.getHome().join(".trash/rafer").mkdirOpt();
        try {
            new Main(console, card, rafs, jpegs, backups, gpxTracks, trash).run();
            return 0;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            console.error.println(e.getMessage());
            e.printStackTrace(console.verbose);
            return 1;
        }
    }

    public static final SimpleDateFormat LINKED_FMT = new SimpleDateFormat("yyMMdd");
    public static final SimpleDateFormat DAY_FMT = new SimpleDateFormat("yyyy/MM/dd");
    public static final SimpleDateFormat MONTH_FMT = new SimpleDateFormat("yyyy/MM");


    private static final String STARTED = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());

    //--

    private final World world;
    private final Console console;
    private final FileNode card;
    // where to store rafs
    private final FileNode rafs;
    private final List<FileNode> backups;
    private final FileNode gpxTracks;
    // where to sto jpegs; may be null
    private final FileNode jpegs;
    private final FileNode inboxTrash;

    public Main(Console console, FileNode card, FileNode rafs, FileNode jpegs, List<FileNode> backups,
                FileNode gpxTracks, FileNode inboxTrash) throws IOException {
        this.world = card.getWorld();
        this.console = console;
        this.card = card;
        this.rafs = rafs;
        this.jpegs = jpegs;
        this.backups = backups;
        this.gpxTracks = gpxTracks;
        this.inboxTrash = inboxTrash;
    }

    public void run() throws IOException {
        Process process;
        int cardCount;
        int backupCount;

        cardCount = 0;
        backupCount = 0;
        directory("rafs", rafs);
        directory("jpegs", jpegs);
        process = new ProcessBuilder("caffeinate").start();
        try {
            if (card.isDirectory()) {
                cardCount++;
                card();
            } else {
                console.info.println("no card");
            }
            inboxSync();
            /* TODO
            for (FileNode backup : backups) {
                if (backup.isDirectory()) {
                    backupCount++;
                    console.info.println("backup sync with " + backup + " ...");
                    backup(rafs, backup);
                } else {
                    console.info.println("backup not available: " + backup);
                }
            }*/
            console.info.println();
            console.info.println("done: card " + cardCount + ", backups: " + backupCount);
        } finally {
            process.destroy();
        }
    }

    public void inboxSync() throws IOException {
        console.info.println("inbox sync ...");
        wipeJpeg();
        // TODO
        // wipeRaf();
        console.info.println("done");
    }

    private static final Date START_DATE;

    static {
        try {
            START_DATE = LINKED_FMT.parse("170101");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    public void wipeJpeg() throws IOException {
        String name;
        FileNode raf;

        for (FileNode jpeg : jpegs.find("**/*" + JPG)) {
            name = jpeg.getName();
            if (getDate(name).before(START_DATE)) {
                // skip
            } else {
                raf = getFile(removeExtension(name), rafs, RAF);
                if (!raf.exists()) {
                    console.info.println("D " + name);
                    inboxTrash(jpeg);
                }
            }
        }
    }

    public void wipeRaf() throws IOException {
        String name;

        for (FileNode raf : rafs.find("**/*.RAF")) {
            name = raf.getName();
            if (!getFile(removeExtension(name), jpegs, JPG).exists()) {
                console.info.println("D " + name);
                inboxTrash(raf);
            }
        }
    }

    private void inboxTrash(FileNode file) throws IOException {
        FileNode dir;

        inboxTrash.mkdirOpt();
        dir = inboxTrash.join(STARTED);
        dir.mkdirOpt();
        file.move(dir.join(file.getName()));
    }

    private void backupTrash(FileNode root, FileNode file) throws IOException {
        FileNode dir;

        dir = root.getParent().join(".trash." + root.getName());
        dir.mkdirOpt();
        dir = dir.join(STARTED);
        dir.mkdirOpt();
        file.move(dir.join(file.getName()));
    }

    private static FileNode getRafFile(String name, FileNode root) {
        return getFile(name, root, RAF);
    }
    private static FileNode getJpgFile(String name, FileNode root) {
        return getFile(name, root, JPG);
    }
    private static FileNode getFile(String name, FileNode root, String ext) {
        return root.join(MONTH_FMT.format(getDate(name)), name + ext);
    }

    private static Date getDate(String name) {
        Date date;

        if (!name.startsWith("r") || name.indexOf('x') != 7) {
            throw new IllegalArgumentException();
        }
        try {
            date = Main.LINKED_FMT.parse(name.substring(1, 7));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
        return date;
    }

    private static String removeExtension(String str) {
        int idx;

        idx = str.lastIndexOf('.');
        if (idx == -1) {
            throw new IllegalArgumentException(str);
        }
        return str.substring(0, idx);
    }

    public void card() throws IOException {
        List<FileNode> cardRafs;
        List<FileNode> downloaded;
        FileNode tmp;
        Map<String, Long> pairs;
        Collection<Long> values;
        long firstTimestamp;
        long lastTimestamp;
        FileNode dcim;

        directory("card", card);
        directory("gpxTracks", gpxTracks);

        tmp = world.getTemp().createTempDirectory();
        dcim = card.join("DCIM");
        cardRafs = findRafs(dcim);
        if (cardRafs.isEmpty()) {
            System.out.println("no images");
            ejectOpt();
            return;
        }
        downloaded = download(cardRafs, tmp);
        onCardBackup(dcim, downloaded);
        ejectOpt();
        pairs = timestamps(tmp);
        values = pairs.values();
        firstTimestamp = Collections.min(values);
        lastTimestamp = Collections.max(values);
        console.info.println("images ranging from " + DAY_FMT.format(new Date(firstTimestamp)) + " to " + DAY_FMT.format(new Date(lastTimestamp)));
        geotags(tmp, firstTimestamp);
        console.info.println("saving jpegs at " + jpegs + " ...");
        moveJpegs(tmp, pairs.keySet());
        console.info.println("saving rafs at " + rafs + " ...");
        moveRafs(tmp, pairs);
        tmp.deleteDirectory(); // it's empty now
    }

    private Map<String, Long> timestamps(FileNode tmp) throws IOException {
        Map<String, Long> result;
        long timestamp;
        String origName;
        String linkedName;

        dates(tmp);
        result = new HashMap<>();
        for (Node raf : tmp.find("*" + RAF)) {
            timestamp = raf.getLastModified();
            origName = Strings.removeRight(raf.getName(), RAF);
            linkedName = linked(origName, timestamp);
            result.put(linkedName, timestamp);
            raf.move(tmp.join(linkedName + RAF));
            tmp.join(origName + JPG).move(tmp.join(linkedName + JPG));
        }
        return result;
    }

    private String linked(String pair, long timestamp) {
        String id;

        id = Strings.removeLeft(pair, "DSCF");
        return "r" + LINKED_FMT.format(new Date(timestamp)) + "x" + id;
    }

    private void moveJpegs(FileNode tmp, Collection<String> names) throws IOException {
        FileNode dest;

        for (String name : names) {
            dest = getJpgFile(name, jpegs);
            dest.getParent().mkdirsOpt();
            tmp.join(name + JPG).move(dest);
        }
    }

    private void moveRafs(FileNode srcDir, Map<String, Long> pairs) throws IOException {
        FileNode src;
        FileNode dest;

        for (Map.Entry<String, Long> entry : pairs.entrySet()) {
            src = srcDir.join(entry.getKey() + RAF);
            dest = rafs.join(MONTH_FMT.format(entry.getValue()), src.getName());
            dest.getParent().mkdirsOpt();
            dest.checkNotExists();
            src.move(dest); // dont copy - disk might be full
            dest.setLastModified(entry.getValue());
        }
    }

    private void backup(FileNode srcRoot, FileNode destRoot) throws IOException {
        String path;

        // add
        for (Node src : srcRoot.find("**/*" + RAF)) {
            path = src.getRelative(srcRoot);
            FileNode dest = destRoot.join(path);
            if (!dest.exists()) {
                dest.getParent().mkdirsOpt();
                console.info.println("A " + destRoot.getName() + "/" + path);
                src.copyFile(dest);
                dest.setLastModified(src.getLastModified());
            }
        }

        for (FileNode dest : destRoot.find("**/*" + RAF)) {
            path = dest.getRelative(destRoot);
            FileNode src = srcRoot.join(path);
            if (!src.exists()) {
                if (src.getParent().exists()) {
                    console.info.println("D " + destRoot.getName() + "/" + path);
                    backupTrash(destRoot, dest);
                } else {
                    // e.g. if src only contains the current year, but the backups hold all the years
                    console.verbose.println("not synced: " + src.getParent());
                }
            }
        }
    }

    /** @return raf nodes with jpg sidecar */
    private List<FileNode> findRafs(FileNode dcim) throws IOException {
        List<FileNode> result;
        String name;
        Filter filter;

        dcim.checkDirectory();
        filter = dcim.getWorld().filter();
        filter.include("**/*.RAF");
        filter.exclude("**/._*.RAF");
        result = dcim.find(filter);
        for (Node raf : result) {
            if (!jpg((FileNode) raf).exists()) {
                throw new IOException("missing jpg for " + raf);
            }
        }
        for (Node other : dcim.find("**/*")) {
            if (other.isDirectory()) {
                // ignore
            } else if (other.getName().startsWith(".")) {
                // ignore, e.g .DS_Store, .dropbox_device, finder previews: ._*
            } else if (!result.contains(other)) {
                name = other.getName();
                if (name.endsWith(JPG) && result.contains(other.getParent().join(Strings.removeRight(name, JPG) + RAF))) {
                    console.verbose.println("found sidecar jpg: " + other);
                } else {
                    throw new IOException("unexpected file in image folder: " + other);
                }
            }
        }
        return result;
    }

    private static void directories(String name, List<FileNode> dirs) throws IOException {
        for (FileNode dir : dirs) {
            directory(name, dir);
        }
    }

    private static void directory(String name, FileNode dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException(name + " not found: " + dir.getAbsolute());
        }
    }

    /** @return srcRafs actually downloaded */
    private List<FileNode> download(List<FileNode> srcRafs, FileNode dest) throws IOException {
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
            destJpg = jpg(destRaf);
            result.add(srcRaf);
            srcRaf.copyFile(destRaf);
            jpg(srcRaf).copyFile(destJpg);
        }
        return result;
    }

    private static FileNode jpg(FileNode raf) {
        return raf.getParent().join(Strings.removeRight(raf.getName(), RAF) + JPG);
    }

    private void ejectOpt() {
        if (card.getParent().getName().equals("Volumes")) {
            eject();
        }
    }

    private void eject() {
        try {
            console.info.println(card.getParent().exec("diskutil", "eject", card.getName()));
        } catch (IOException e) {
            console.info.println("WARNING: eject failed");
        }
    }

    private void onCardBackup(FileNode src, List<FileNode> srcRafs) throws IOException {
        FileNode downloaded;
        String path;
        FileNode destRaf;

        downloaded = card.join("DOWNLOADED");
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
            jpg(srcRaf).move(jpg(destRaf));
        }
        if (findRafs(src).isEmpty()) {
            console.info.println("complete download, removing " + src);
        }
    }

    private void geotags(FileNode dir, long firstTimestamp) throws IOException {
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
        launcher.arg("-ext", RAF);
        launcher.arg("-ext", JPG);
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

        launcher = new Launcher(dir, "exiftool", "-FileModifyDate<DateTimeOriginal", ".", "-ext", RAF);
        launcher.exec(console.info);
    }
}
