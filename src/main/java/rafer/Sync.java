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

import net.mlhartme.smuggler.cache.FolderData;
import net.mlhartme.smuggler.cache.ImageData;
import net.mlhartme.smuggler.cli.Config;
import net.mlhartme.smuggler.smugmug.Account;
import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import rafer.model.Archive;
import rafer.model.Pairs;
import rafer.model.Utils;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Sync {
    private final World world;
    private final Console console;
    private final rafer.model.Config config;

    public Sync(World world, Console console) throws MkdirException {
        this(world, console, new rafer.model.Config(world));
    }

    public Sync(World world, Console console, rafer.model.Config config) {
        this.world = world;
        this.console = console;
        this.config = config;
    }

    public void run() throws IOException {
        Process process;
        int cardCount;
        int backupCount;
        int errors;
        Account smugmugAccount;
        FolderData smugmugRoot;
        FileNode tmp;

        cardCount = 0;
        backupCount = 0;
        if (config.rafs.available()) {
            throw new IOException("local archive not available");
        }
        if (config.smugmug != null) {
            config.smugmug.checkFile();
            smugmugAccount = Config.load(world).newSmugmug(world);
            smugmugRoot = FolderData.load(config.smugmug);
        } else {
            smugmugAccount = null;
            smugmugRoot = null;
        }
        process = new ProcessBuilder("caffeinate").start();
        try {
            if (config.card.available()) {
                cardCount++;
                tmp = world.getTemp().createTempDirectory();
                List<FileNode> downloaded = config.card.download(console, tmp);
                if (downloaded.isEmpty()) {
                    console.info.println("no images");
                } else {
                    Pairs pairs;

                    pairs = Pairs.normalize(console, tmp);
                    console.info.println("adding geotags ...");
                    pairs.geotags(console, config.gpxTracks);
                    console.info.println("saving rafs at " + config.rafs + " ...");
                    pairs.moveRafs(config.rafs);
                    console.info.println("smugmug upload ...");
                    pairs.smugmugUpload(smugmugAccount, smugmugRoot);
                    tmp.deleteDirectory();
                }
            } else {
                console.info.println("no card");
            }
            smugmugSync(smugmugAccount, smugmugRoot);
            for (Archive backup : config.backups) {
                if (backup.available()) {
                    backupCount++;
                    console.info.println("backup sync with " + backup + " ...");
                    errors = backup.add(console, config.rafs);
                    if (errors > 0) {
                        console.info.println("# errors: " + errors);
                    }
                } else {
                    console.info.println("backup not available: " + backup);
                }
            }
            console.info.println();
            console.info.println("done: card " + cardCount + ", backups: " + backupCount);
        } finally {
            if (smugmugAccount != null) {
                smugmugRoot.sort();
                config.smugmug.writeString(smugmugRoot.toString());
            }
            process.destroy();
        }
    }

    public void smugmugSync(Account account, FolderData root) throws IOException {
        console.info.println("smugmug sync ...");
        smugmugDeletes(account, root);
        console.info.println("done");
    }

    public static final Date START_DATE;

    static {
        try {
            START_DATE = Utils.LINKED_FMT.parse("170101");
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    public void smugmugDeletes(Account account, FolderData root) throws IOException {
        FileNode raf;
        Map<String, ImageData> remoteMap;
        String name;
        ImageData image;

        remoteMap = new HashMap<>();
        root.imageMap(remoteMap);
        for (Map.Entry<String, ImageData> entry : remoteMap.entrySet()) {
            name = entry.getKey();
            image = entry.getValue();
            if (getDate(name).before(START_DATE)) {
                // skip
            } else {
                raf = config.rafs.getFile(removeExtension(name), Utils.RAF);
                if (!raf.exists()) {
                    console.info.println("D " + name);
                    account.albumImage(image.uri).delete();
                    if (!image.album.images.remove(image)) {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }

    public static FileNode getJpgFile(String name, FileNode root) {
        return getFile(name, root, Utils.JPG);
    }
    public static FileNode getFile(String name, FileNode root, String ext) {
        return root.join(Utils.MONTH_FMT.format(getDate(name)), name + ext);
    }

    public static Date getDate(String name) {
        Date date;

        if (!name.startsWith("r") || name.indexOf('x') != 7) {
            throw new IllegalArgumentException();
        }
        try {
            date = Utils.LINKED_FMT.parse(name.substring(1, 7));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
        return date;
    }

    public static String removeExtension(String str) {
        int idx;

        idx = str.lastIndexOf('.');
        if (idx == -1) {
            throw new IllegalArgumentException(str);
        }
        return str.substring(0, idx);
    }

    public static void directory(String name, FileNode dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException(name + " not found: " + dir.getAbsolute());
        }
    }

    public static FileNode jpg(FileNode raf) {
        return raf.getParent().join(Strings.removeRight(raf.getName(), Utils.RAF) + Utils.JPG);
    }
}
