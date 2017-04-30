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

import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/** Creates Index for a backup tree */
public class Index implements Iterable<String> {
    public static Index scan(FileNode dir) throws IOException {
        Index index;
        String path;

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
        return index;
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

    /** maps paths to md5 */
    private Map<String, String> lines;

    public Index() {
        lines = new TreeMap<>();
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

    public String toString() {
        StringBuilder result;

        result = new StringBuilder();
        for (Map.Entry<String, String> entry : lines.entrySet()) {
            result.append(entry.getKey()).append(' ').append(entry.getValue()).append('\n');
        }
        return result.toString();
    }

    public void save(FileNode dir) throws IOException {
        file(dir).writeString(toString());
    }

    @Override
    public Iterator<String> iterator() {
        return new HashSet<>(lines.keySet()).iterator();
    }
}
