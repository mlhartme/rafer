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

public class Clean {
    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode dir;

        world = World.create();
        dir = world.file("/Volumes/Neuerkeller/Bilder");
        for (FileNode d : dir.find("*/??/??")) {
            d.checkDirectory();
            for (FileNode xmp : d.find("*.xmp")) {
                System.out.println("xmp: " + xmp);
                xmp.deleteFile();
            }
            if (d.list().isEmpty()) {
                System.out.println("empty: " + d);
                d.deleteDirectory();
            }
        }
    }
}
