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
import java.util.Date;

public class Sort {
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode src;
        FileNode dest;
        String name;
        Date date;
        FileNode file;

        world = World.create();
        src = world.getHome().join("Timeline/Album");
        dest = world.getHome().join("Timeline");
        for (FileNode jpg : src.find("r*x*.JPG")) {
            name = jpg.getName();
            if (!name.startsWith("r") || name.indexOf('x') != 7) {
                throw new IllegalStateException();
            }
            date = Main.LINKED_FMT.parse(name.substring(1, 7));
            file = dest.join(Main.JPG_FMT.format(date), name);
            System.out.println(name + " -> " + file);
            file.getParent().mkdirsOpt();
            jpg.move(file);
        }
    }
}
