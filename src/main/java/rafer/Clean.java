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
import net.oneandone.sushi.util.Strings;
import rafer.model.Utils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Clean {
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode dir;
        String name;
        int idx;
        String ext;
        Set<String> extensions = new HashSet<>();
        String full;

        world = World.create();
        dir = world.file("/Volumes/Data/Bilder");
        //dir = world.file("/Volumes/Neuerkeller/Bilder");
        FileNode todo = dir.join("TODO-created-date");
        Date HEUTE = Utils.LINKED_FMT.parse("170501");
        int count = 0;
        for (FileNode month : dir.find("*/??")) {
            month.checkDirectory();
            for (FileNode image : month.list()) {
                count++;
                name = image.getName();
                if (name.equals("CaptureOne")) {
                    continue;
                }
                image.checkFile();
                if (image.getName().equals(".DS_Store")) {
                    continue;
                }
                idx = name.lastIndexOf('.');
                if (idx == -1) {
                    throw new IOException(name);
                }
                ext = name.substring(idx + 1);
                extensions.add(ext);
                name = name.substring(0, idx);
                if (!"JPG".equals(ext)) {
                    continue; // TODO
                }
                if (!name.startsWith("r") || name.charAt(7) != 'x') {
                    throw new IOException("unexpected file name: " + name);
                }

                Date date = new Date(image.getLastModified());
                if (date.after(HEUTE))  {
                    move(image, todo.join(image.getParent().getParent().getName(), image.getParent().getName(), image.getName()));
                } else {
                    full = "r" + Utils.LINKED_FMT.format(date) + "x" + image.getName().substring(8);
                    FileNode dest = dir.join(Utils.MONTH_FMT.format(date), full);
                    if (!image.equals(dest)) {
                        move(image, dest);
                    }
                }
            }
        }
        System.out.println("count: " + count);
        System.out.println("extensions: " + extensions);
    }

    private static void move(FileNode src, FileNode dest) throws IOException {
        String name;
        int idx;
        String ext;
        char c;

        System.out.println(src + " -> " + dest);
        name = dest.getName();
        idx = name.lastIndexOf('.');
        if (idx == -1) {
            throw new IOException(name);
        }
        ext = name.substring(idx + 1);
        name = name.substring(0, idx);
        c = 'a';
        while (dest.exists()) {
            dest = dest.getParent().join(name + c + "." + ext);
            c++;
        }
        dest.getParent().mkdirsOpt();
        src.move(dest);
    }
}
