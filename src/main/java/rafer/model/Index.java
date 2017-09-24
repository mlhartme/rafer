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
package rafer.model;

import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.*;

/** Creates Index for a backup tree */
public class Index implements Iterable<String> {
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
                idx = line.lastIndexOf(' ');
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

    public Index(Index index) {
        lines = new TreeMap<>(index.lines);
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

    public String get(String path) {
        return lines.get(path);
    }

    public int hashCode() {
        return lines.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Index) {
            return lines.equals(((Index) obj).lines);
        }
        return false;
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

    public void bak(FileNode dir) throws IOException {
        FileNode file;
        FileNode bak;

        file = file(dir);
        if (file.exists()) {
            bak = file.getParent().join(file.getName() + ".bak");
            if (bak.exists()) {
                bak.deleteFile();
            }
            file.copyFile(bak);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new HashSet<>(lines.keySet()).iterator();
    }

    public Collection<String> extensions() {
        Set<String> result;
        int idx;

        result = new HashSet();
        for (String path : lines.keySet()) {
            idx = path.lastIndexOf('.');
            result.add(path.substring(idx + 1));
        }
        return result;
    }

    public int size() {
        return lines.size();
    }

    //--

    public Patch verify(FileNode dir, boolean md5) throws IOException {
        Patch result;
        String old;
        HashSet<String> existing;

        result = new Patch();
        existing = new HashSet<>();
        for (FileNode file : dir.find("**/*")) {
            String path;

            if (file.isDirectory()) {
                continue;
            }
            path = file.getRelative(dir);
            if (ignored(path)) {
                continue;
            }
            old =  lines.get(path);
            if (old == null) {
                result.add(new Action("A " + path) {
                    @Override
                    public void invoke() throws IOException {
                        Index.this.put(path, file.md5());
                    }
                });
            } else {
                existing.add(path);
                if (md5) {
                    final String checksum = file.md5();
                    if (!old.equals(checksum)) {
                        result.add(new Action("U " + path) {
                            @Override
                            public void invoke() throws IOException {
                                Index.this.put(path, checksum);
                            }
                        });
                    }
                }
            }
        }
        for (String path : lines.keySet()) {
            if (!existing.contains(path)) {
                result.add(new Action("D " + path) {
                    @Override
                    public void invoke() throws IOException {
                        Index.this.remove(path);
                    }
                });
            }
        }
        return result;
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

}
