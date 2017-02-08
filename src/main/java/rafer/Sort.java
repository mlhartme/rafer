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

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Sort {
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode dir;
        Index idx;
        List<String> remove;
        Set<String> suffixes;
        int i;

        world = World.create();
        dir = world.file("/Volumes/Data/Bilder");
        //dir = world.file("/Volumes/Neuerkeller/Bilder");
        idx = Index.load(dir);
        remove = new ArrayList<>();
        suffixes = new HashSet<>();
        for (String path : idx) {
            if (Index.ignored(path)) {
                System.out.println(path);
                remove.add(path);
            }
            i = path.lastIndexOf('.');
            if (i != -1) {
                suffixes.add(path.substring(i + 1));
            }
        }
        System.out.println("suffixes: " + suffixes);
        for (String path : remove) {
            idx.remove(path);
        }
        System.out.println(idx.save(dir));
    }

    public static void sort(String[] args) throws IOException, ParseException {
        World world;
        FileNode src;
        FileNode dest;
        String name;
        Date date;
        FileNode file;

        world = World.create();
        src = world.getHome().join("Desktop/todobilder");
        src.checkDirectory();
        dest = world.getHome().join("Timeline");
        dest.checkDirectory();
        for (FileNode raf : src.find("**/*.JPG")) {
            name = raf.getName();
            if (!name.startsWith("r") || name.indexOf('x') != 7) {
                throw new IllegalStateException();
            }
            date = Main.LINKED_FMT.parse(name.substring(1, 7));
            file = dest.join(Main.MONTH_FMT.format(date), name);
            System.out.println(name + " -> " + file);
            raf.move(file);
        }
    }
}
