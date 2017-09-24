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

import net.mlhartme.smuggler.cache.AlbumData;
import net.mlhartme.smuggler.cache.FolderData;
import net.mlhartme.smuggler.cache.ImageData;
import net.mlhartme.smuggler.cli.Config;
import net.mlhartme.smuggler.smugmug.Account;
import net.mlhartme.smuggler.smugmug.Album;
import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import rafer.model.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Smugmug extends Base {
    private final World world;
    private final rafer.model.Config config;

    public Smugmug(World world, Console console) throws IOException {
        this(world, console, rafer.model.Config.load(world));
    }

    public Smugmug(World world, Console console, rafer.model.Config config) {
        super(console, true);
        this.world = world;
        this.config = config;
    }

    public void doRun() throws IOException {
        Account smugmugAccount;
        FolderData smugmugRoot;
        Volume localVolume;
        List<Volume> backups;

        backups = new ArrayList<>(config.volumes);
        localVolume = backups.remove(0);
        if (!localVolume.available()) {
            throw new IOException("local archive not available");
        }
        if (config.smugmug == null) {
            throw new IOException("smugmug not configured");
        }
        config.smugmug.checkFile();
        smugmugAccount = Config.load(world).newSmugmug(world);
        smugmugRoot = FolderData.load(config.smugmug);
        try (Archive local = localVolume.open()) {
            sync(smugmugAccount, smugmugRoot, local);
        } finally {
            smugmugRoot.sort();
            config.smugmug.writeString(smugmugRoot.toString());
        }
    }

    public void sync(Account account, FolderData root, Archive local) throws IOException {
        console.info.println("smugmug sync ...");
        deletes(account, root, local);
        updates(account, root, local);
        console.info.println("done");
    }

    public void updates(Account account, FolderData root, Archive local) throws IOException {
        FileNode parent;
        Map<String, ImageData> remoteMap;
        String name;
        ImageData image;
        AlbumData album;
        String md5;
        Set<String> names;

        remoteMap = new HashMap<>();
        root.imageMap(remoteMap);
        remoteMap = toLowerCase(remoteMap);
        names = local.names();
        for (FileNode jpg : config.smugmugInbox.list()) {
            jpg.checkFile();
            name = jpg.getName();
            if (names.contains(removeExtension(name))) {
                parent = local.getFile(Utils.getParent(removeExtension(name)));
                image = remoteMap.get(name.toLowerCase());
                if (image == null) {
                    console.info.println("A " + name);
                    root.getOrCreateAlbum(account, parent.getRelative(local.directory)).upload(account, jpg);
                } else {
                    md5 = jpg.md5();
                    if (image.md5.equals(md5)) {
                        console.info.println("  " + name);
                    } else {
                        console.info.println("U " + name);
                        album = root.getOrCreateAlbum(account, parent.getRelative(local.directory));
                        if (!album.images.remove(image)) {
                            throw new IllegalStateException();
                        }
                        album.upload(account, jpg);
                    }
                }
                jpg.deleteFile();
            } else {
                // inbox file is unknown - ignore
                console.info.println("! " + name);
            }
        }
    }

    private  Map<String, ImageData> toLowerCase(Map<String, ImageData> remoteMap) {
        Map<String, ImageData> result;

        result = new HashMap<>(remoteMap.size());
        for (Map.Entry<String, ImageData> entry : remoteMap.entrySet()) {
            result.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return result;
    }

    public void deletes(Account account, FolderData root, Archive local) throws IOException {
        Map<String, ImageData> remoteMap;
        String name;
        ImageData image;
        Set<String> names;

        names = local.names();
        remoteMap = new HashMap<>();
        root.imageMap(remoteMap);
        for (Map.Entry<String, ImageData> entry : remoteMap.entrySet()) {
            name = entry.getKey();
            image = entry.getValue();
            if (!names.contains(removeExtension(name))) {
                console.info.println("D " + name);
                account.albumImage(image.uri).delete();
                if (!image.album.images.remove(image)) {
                    throw new IllegalStateException();
                }
            }
        }
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
