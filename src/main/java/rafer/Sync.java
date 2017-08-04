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
import rafer.model.Patch;
import rafer.model.Utils;
import rafer.model.Volume;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Sync {
    private final World world;
    private final Console console;
    private final rafer.model.Config config;

    public Sync(World world, Console console) throws IOException {
        this(world, console, rafer.model.Config.load(world));
    }

    public Sync(World world, Console console, rafer.model.Config config) {
        this.world = world;
        this.console = console;
        this.config = config;
    }

    public void run() throws IOException {
        Process process;
        int backupCount;
        Volume localVolume;
        List<Volume> backups;

        backupCount = 0;
        backups = new ArrayList<>(config.volumes);
        localVolume = backups.remove(0);
        if (!localVolume.available()) {
            throw new IOException("local archive not available");
        }
        process = new ProcessBuilder("caffeinate").start();
        try (Archive local = localVolume.open()) {
            for (Volume volume : backups) {
                if (volume.available()) {
                    backupCount++;
                    try (Archive archive = volume.open()) {
                        console.info.println("backup sync with " + volume + " ...");

                        Patch patch = archive.diff(local);
                        console.info.println(patch);
                        patch.applyAndReport(console);
                    }
                } else {
                    console.info.println("volume not available: " + volume);
                }
            }
            console.info.println();
            console.info.println("done: backups: " + backupCount);
        }
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
