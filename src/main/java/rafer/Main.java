/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static final String RAF = ".RAF";
    public static final String JPG = ".JPG";

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String ... args) {
        World world;
        Console console;
        FileNode card;
        FileNode dest;
        List<FileNode> backups;
        FileNode gpxTracks;

        world = new World();
        console = Console.create(world);
        if (args.length != 0) {
            console.error.println("unexpected argument(s)");
            return 1;
        }
        card = world.file("/Volumes/UNTITLED");
        dest = (FileNode) console.world.getHome().join("Pictures/Rafer");
        backups = new ArrayList<>();
        backups.add(console.world.file("/Volumes/Data/Bilder"));
        backups.add(console.world.file("/Volumes/Elements3T/Bilder"));
        gpxTracks = (FileNode) console.world.getHome().join("Dropbox/Apps/Geotag Photos Pro (iOS)");
        try {
            new Main(console, card, dest, backups, gpxTracks, true).run();
            return 0;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            console.error.println(e.getMessage());
            e.printStackTrace(console.verbose);
            return 1;
        }
    }

    public static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy/MM/dd");
    public static final SimpleDateFormat LINKED_FMT = new SimpleDateFormat("yyMMdd");

    //--

    private final Console console;
    private final FileNode card;
    // where to store raws
    private final FileNode raws;
    private final List<FileNode> backups;
    private final FileNode gpxTracks;
    private final boolean cloud;

    public Main(Console console, FileNode card, FileNode destDng, List<FileNode> backups,
                FileNode gpxTracks, boolean cloud) throws IOException {
        this.console = console;
        // no card, no fun
        this.card = card;
        this.raws = destDng;
        this.backups = backups;
        this.gpxTracks = gpxTracks;
        this.cloud = cloud;
    }

    public void run() throws IOException {
        FileNode tmp;
        Map<String, Long> pairs;
        Collection<Long> values;
        long firstTimestamp;
        long lastTimestamp;
        FileNode dcim;
        Process process;

        directory("card", card);
        directory("raws", raws);
        directories("backup", backups);
        directory("gpxTracks", gpxTracks);

        process = new ProcessBuilder("caffeinate").start();
        try {
            tmp = console.world.getTemp().createTempDirectory();

            dcim = card.join("DCIM");
            if (download(findRafs(dcim), tmp).isEmpty()) {
                console.info.println("no images");
                ejectOpt();
                return;
            }
            onCardBackup(dcim);
            ejectOpt();
            pairs = timestamps(tmp);
            values = pairs.values();
            firstTimestamp = Collections.min(values);
            lastTimestamp = Collections.max(values);
            console.info.println("images ranging from " + FMT.format(new Date(firstTimestamp)) + " to " + FMT.format(new Date(lastTimestamp)));
            geotags(tmp, firstTimestamp);
            console.info.println("saving raws at " + raws + " ...");
            saveRaws(tmp, pairs);
            if (cloud) {
                console.info.println("add to cloud ...");
                cloud(tmp, pairs.keySet());
            }
            for (FileNode backup : backups) {
                console.info.println("backup raws to " + backup + " ...");
                backup(raws, backup);
            }
            tmp.deleteTree();
        } finally {
            process.destroy();
        }
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

    private void cloud(FileNode tmp, Collection<String> names) throws Failure, MoveException {
        Launcher launcher;

        launcher = tmp.launcher("osascript", "/Users/mhm/Projects/foss/rafer/addToCloud");
        for (String name : names) {
            launcher.arg(tmp.join(name + JPG).getURI().toString());
        }
        console.verbose.println(launcher);
        launcher.exec(console.info);
    }

    private void saveRaws(FileNode srcDir, Map<String, Long> pairs) throws IOException {
        FileNode src;
        FileNode dest;

        for (Map.Entry<String, Long> entry : pairs.entrySet()) {
            src = srcDir.join(entry.getKey() + RAF);
            dest = raws.join(FMT.format(entry.getValue()), src.getName());
            dest.getParent().mkdirsOpt();
            dest.checkNotExists();
            src.move(dest); // dont copy - disk might be full
            dest.setLastModified(entry.getValue());
        }
    }

    private void backup(FileNode srcRoot, FileNode destRoot) throws IOException {
        String path;

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

        for (Node dest : destRoot.find("**/*" + RAF)) {
            path = dest.getRelative(destRoot);
            FileNode src = srcRoot.join(path);
            if (!src.exists()) {
                if (src.getParent().exists()) {
                    console.info.println("D " + destRoot.getName() + "/" + path);
                    dest.deleteFile();
                } else {
                    // e.g. if src only contains the current year, but the backups hold all the years
                    console.info.println("not synced: " + src.getParent());
                }
            }
        }
    }

    /** @return raf nodes with jpg sidecar */
    private List<Node> findRafs(FileNode dcim) throws IOException {
        List<Node> result;
        String name;

        dcim.checkDirectory();
        result = dcim.find("**/*.RAF");
        for (Node raf : result) {
            if (!jpg((FileNode) raf).exists()) {
                throw new IOException("missing jpg for " + raf);
            }
        }
        if (result.isEmpty()) {
            throw new IOException("no images in " + dcim);
        }
        for (Node other : dcim.find("**/*")) {
            if (other.isDirectory()) {
                // ignore
            } else if (other.getName().startsWith(".")) {
                // ignore, e.g .DS_Store, .dropbox_device
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

    /** @return names or raf- and jpg pairs */
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
            jpg((FileNode) srcRaf).copyFile(jpg(destRaf));
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
