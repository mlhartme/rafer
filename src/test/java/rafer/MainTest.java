/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
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


import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MainTest {
    @Test
    public void foo() throws IOException {
        World world;
        FileNode home;
        FileNode root;
        Main main;
        FileNode card;
        FileNode dest;
        FileNode rafs;
        FileNode backup;
        FileNode gpxTracks;

        world = new World();
        home = world.guessProjectHome(MainTest.class);
        root = home.join("target/it");
        root.deleteTreeOpt();
        root.mkdirsOpt();
        card = root.join("card").mkdir();
        dest = root.join("dest");
        rafs = home.join("src/test/rafs");
        rafs.copyDirectory(card.join("DCIM").mkdir());
        backup = root.join("backup").mkdir();
        gpxTracks = root.join("gpxTracks").mkdir();
        backup.join("foo.dng").mkfile();
        main = new Main(Console.create(world), card, dest.mkdir(), backup, gpxTracks);
        main.run();
        assertEquals(rafs.list().size() / 2, dest.find("**/*.dng").size());
    }
}
