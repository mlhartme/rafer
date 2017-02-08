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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Diff;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.*;

/** Creates Index for a backup tree */
public class Index implements Iterable<String> {
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode dir;
        Index index;
        String path;

        world = World.create();
        dir = world.file("/Volumes/Data/Bilder");
        index = new Index();
        for (FileNode file : dir.find("**/*")) {
            if (file.isDirectory()) {
                continue;
            }
            path = file.getRelative(dir);
            if (ignored(path)) {
                continue;
            }
            try {
                index.put(path, file.md5());
            } catch (IOException e) {
                System.out.println(file + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println(index.save(dir));
    }

    public static boolean ignored(String path) {
        String name;

        if (path.contains("/CaptureOne/Cache/")) {
            return true;
        }
        name = path.substring(path.lastIndexOf('/') + 1); // ok for not found
        if (name.startsWith(".")) {
            return true;
        }
        return false;
    }

    //--

    public static FileNode file(FileNode dir) {
        return dir.join(".index");
    }

    public static Index load(FileNode dir) throws IOException {
        int idx;
        Index result;
        FileNode file;

        result = new Index();
        file = file(dir);
        if (file.exists()) {
            for (String line : file.readLines()) {
                idx = line.indexOf(' ');
                result.put(line.substring(0, idx), line.substring(idx + 1));
            }
        }
        return result;
    }

    private Map<String, String> lines;

    public Index() {
        lines = new HashMap<>();
    }

    public void put(String path, String hash) {
        lines.put(path, hash);
    }

    public void remove(String path) {
        lines.remove(path);
    }

    public boolean contains(String path) {
        return lines.containsKey(path);
    }

    /** @return diff */
    public String save(FileNode dir) throws IOException {
        FileNode idx;
        String prev;
        List<String> keys;

        idx = file(dir);
        if (idx.exists()) {
            prev = idx.readString();
        } else {
            prev = "";
        }
        keys = new ArrayList<>(lines.keySet());
        Collections.sort(keys);
        try (Writer dest = idx.newWriter()) {
            for (String key : keys) {
                dest.write(key);
                dest.write(' ');
                dest.write(lines.get(key));
                dest.write('\n');
            }
        }
        return "TODO: diff"; // Diff.diff(prev, idx.readString());
    }

    @Override
    public Iterator<String> iterator() {
        return lines.keySet().iterator();
    }
}
