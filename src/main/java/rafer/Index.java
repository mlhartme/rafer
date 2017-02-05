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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Creates Index for a backup tree */
public class Index {
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode dir;
        List<String> lines;
        List<String> prev;
        FileNode idx;

        world = World.create();
        dir = world.file("/Volumes/Data/Bilder");
        lines = new ArrayList<>();
        for (FileNode file : dir.find("**/*")) {
            if (file.isFile() && !file.getName().startsWith(".")) {
                try {
                    lines.add(file.getRelative(dir) + " " + file.md5());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.sort(lines);
        idx = dir.join(".index");
        if (idx.exists()) {
            prev = idx.readLines();
        } else {
            prev = new ArrayList<>();
        }
        idx.writeLines(lines);
        System.out.println(Diff.diff(prev, lines, false, 0, false));
    }
}
