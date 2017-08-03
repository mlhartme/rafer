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
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import rafer.model.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Import {
    private final World world;
    private final Console console;
    private final rafer.model.Config config;

    public Import(World world, Console console) throws IOException {
        this(world, console, rafer.model.Config.load(world));
    }

    public Import(World world, Console console, rafer.model.Config config) {
        this.world = world;
        this.console = console;
        this.config = config;
    }

    public void run() throws IOException {
        Process process;
        FileNode tmp;
        Volume localVolume;
        Pairs pairs;

        localVolume = config.volumes.get(0);
        if (!localVolume.available()) {
            throw new IOException("local archive not available");
        }
        if (!config.card.available()) {
            throw new IOException("no card");
        }
        process = new ProcessBuilder("caffeinate").start();
        try (Archive local = localVolume.open()) {
            tmp = world.getTemp().createTempDirectory();
            if (!config.card.download(console, tmp)) {
                console.info.println("no images");
                return;
            }
            pairs = Pairs.normalize(console, tmp);
            console.info.println("adding geotags ...");
            pairs.geotags(console, config.gpxTracks);
            console.info.println("saving rafs at " + localVolume + " ...");
            pairs.moveRafs(local);
            console.info.println("moving jpgs to smugmug inbox ...");
            pairs.smugmugInbox(config.smugmugInbox);
            tmp.deleteDirectory();
        } finally {
            process.destroy();
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
            throw new IllegalArgumentException("not a lined name: " + name);
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
