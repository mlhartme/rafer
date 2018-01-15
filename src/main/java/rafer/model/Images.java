package rafer.model;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Directory with id-names images. */
public class Images {
    /** set modified timestamps; renames to linked names */
    public static Images normalize(Console console, FileNode directory) throws IOException {
        Map<String, Long> result;
        long timestamp;
        String origName;
        String idName;

        dates(console, directory);
        result = new HashMap<>();
        for (Node image : directory.list()) {
            timestamp = image.getLastModified();
            origName = image.getName();
            idName = idName(origName, timestamp);
            idName = unique(directory, idName);
            result.put(idName, timestamp);
            image.move(directory.join(idName));
        }
        return new Images(directory, result);
    }

    private static String unique(FileNode directory, String idName) {
        while (true) {
            if (!directory.join(idName).exists()) {
                return idName;
            }
            idName = withoutExt(idName) + "z" + ext(idName);
        }
    }

    private static String withoutExt(String str) {
        int idx = str.lastIndexOf('.');
        return idx == - 1 ? str : str.substring(0, idx);
    }

    private static String ext(String str) {
        int idx = str.lastIndexOf('.');
        return idx == - 1 ? "" : str.substring(idx);
    }

    private static String idName(String name, long timestamp) {
        String id;

        name = name.toUpperCase();
        if (name.startsWith("DSCF")) {
            id = Strings.removeLeft(name, "DSCF");
        } else if (name.startsWith("r") && name.length() > 7 && name.charAt(7) == 'x') {
            id = name.substring(8);
        } else {
            id = name;
        }
        return "r" + Utils.LINKED_FMT.format(new Date(timestamp)) + "x" + id;
    }


    private static void dates(Console console, FileNode dir) throws IOException {
        Launcher launcher;

        // make sure that files without DateTimeOriginal end up in January 1970
        for (FileNode file : dir.list()) {
            file.setLastModified(0);
        }
        launcher = new Launcher(dir, "exiftool", "-FileModifyDate<DateTimeOriginal", ".");
        launcher.exec(console.info);
    }

    //--

    private final FileNode directory;

    /** maps file name (without extension to modified date */
    private final Map<String, Long> images;

    private Images(FileNode directory, Map<String, Long> images) {
        this.directory = directory;
        this.images = images;
    }

    //--

    public void geotags(Console console, FileNode gpxTracks) throws IOException {
        Collection<Long> values;
        long firstTimestamp;
        long lastTimestamp;

        Utils.directory("gpxTracks", gpxTracks);
        values = images.values();
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
        launcher.arg("-Artist=Michael Hartmeier", "-Copyright=Copyright Michael Hartmeier, all rights reserved");
        launcher.arg("-P", "-api", "GeoMaxIntSecs=43200"); // 12 Stunden, weil er keine neuen Punkte speichert, wenn man sich nicht bewegt
        for (FileNode track : tracks) {
            launcher.arg("-geotag");
            launcher.arg(track.getAbsolute());
        }
        launcher.arg(".");
        if (tracks.isEmpty()) {
            console.info.println("no matching gpx files");
        } else {
            console.info.println(launcher.toString());
            launcher.exec(console.info);
        }
    }

    //--

    public void archive(Archive dest) throws IOException {
        String name;
        FileNode src;
        long modified;

        for (Map.Entry<String, Long> entry : images.entrySet()) {
            name = entry.getKey();
            src = directory.join(name);
            if (isRendering(name)) {
            } else {
                modified = entry.getValue();
                dest.moveInto(src, Utils.MONTH_FMT.format(modified) + "/" + src.getName());
            }
        }
    }

    private boolean isRendering(String cmp) {
        String base;

        if (cmp.endsWith(Utils.JPG)) {
            base = removeExtension(cmp);
            for (String name : images.keySet()) {
                if (!cmp.equals(name) && base.equals(removeExtension(name))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String removeExtension(String name) {
        int idx;

        idx = name.lastIndexOf('.');
        return idx == -1 ? name : name.substring(0, idx);
    }

    public void smugmugInbox(FileNode smugmugInbox) throws IOException {
        FileNode src;
        FileNode dest;

        for (String name : images.keySet()) {
            if (name.endsWith(Utils.JPG)) {
                src = directory.join(name);
                dest = smugmugInbox.join(name);
                dest.checkNotExists();
                src.move(dest);
            }
        }
    }
}
