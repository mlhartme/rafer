package rafer.model;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;
import rafer.Sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Directory with pair of raf- and jpg files */
public class Pairs {
    /** set modified timestamps; renames to linked names */
    public static Pairs normalize(Console console, FileNode directory) throws IOException {
        Map<String, Long> result;
        long timestamp;
        String origName;
        String linkedName;

        dates(console, directory);
        result = new HashMap<>();
        for (Node raf : directory.find("*" + Utils.RAF)) {
            timestamp = raf.getLastModified();
            origName = Strings.removeRight(raf.getName(), Utils.RAF);
            linkedName = linked(origName, timestamp);
            result.put(linkedName, timestamp);
            raf.move(directory.join(linkedName + Utils.RAF));
            directory.join(origName + Utils.JPG).move(directory.join(linkedName + Utils.JPG));
        }
        return new Pairs(directory, result);
    }

    private static String linked(String pair, long timestamp) {
        String id;

        id = Strings.removeLeft(pair, "DSCF");
        return "r" + Utils.LINKED_FMT.format(new Date(timestamp)) + "x" + id;
    }


    private static void dates(Console console, FileNode dir) throws IOException {
        Launcher launcher;

        launcher = new Launcher(dir, "exiftool", "-FileModifyDate<DateTimeOriginal", ".", "-ext", Utils.RAF);
        launcher.exec(console.info);
    }

    //--

    private final FileNode directory;
    private final Map<String, Long> pairs;

    private Pairs(FileNode directory, Map<String, Long> pairs) {
        this.directory = directory;
        this.pairs = pairs;
    }

    //--

    public void geotags(Console console, FileNode gpxTracks) throws IOException {
        Collection<Long> values;
        long firstTimestamp;
        long lastTimestamp;

        Sync.directory("gpxTracks", gpxTracks);
        values = pairs.values();
        firstTimestamp = Collections.min(values);
        lastTimestamp = Collections.max(values);
        console.info.println("images ranging from " + Utils.DAY_FMT.format(new Date(firstTimestamp)) + " to " + Utils.DAY_FMT.format(new Date(lastTimestamp)));
        geotags(console, gpxTracks, directory, firstTimestamp);
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

    //--

    public void moveRafs(Archive dest) throws IOException {
        FileNode src;

        for (Map.Entry<String, Long> entry : pairs.entrySet()) {
            src = directory.join(entry.getKey() + Utils.RAF);
            dest.moveInto(src);
        }
    }

    public void smugmugInbox(FileNode smugmugInbox) throws IOException {
        FileNode src;
        FileNode dest;

        for (String name : pairs.keySet()) {
            src = Sync.getJpgFile(name, directory);
            dest = smugmugInbox.join(src.getName());
            dest.checkNotExists();
            src.move(dest);
        }
    }

}
