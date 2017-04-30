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


import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SyncTest {
    @Test
    public void foo() throws IOException {
        World world;
        FileNode home;
        FileNode root;
        Sync sync;
        FileNode card;
        FileNode raws;
        FileNode jpegs;
        FileNode src;
        FileNode backup;
        FileNode gpxTracks;
        FileNode trash;

        world = World.create();
        home = world.guessProjectHome(SyncTest.class);
        src = home.join("src/test/card");
        root = home.join("target/it");
        root.deleteTreeOpt();
        root.mkdirsOpt();
        card = root.join("card").mkdir();
        src.copyDirectory(card.join("DCIM").mkdir());
        raws = root.join("raws").mkdir();
        jpegs = root.join("jpegs").mkdir();
        backup = root.join("backup").mkdir();
        gpxTracks = root.join("gpxTracks").mkdir();
        trash = root.join("trash").mkdir();
        backup.join("foo.RAF").mkfile();
        sync = new Sync(world, Console.create(), card, raws, jpegs, Collections.singletonList(backup), gpxTracks, trash);
        sync.run();
        assertEquals(src.list().size() / 2, raws.find("**/*.RAF").size());
        assertEquals(src.list().size() / 2, jpegs.find("**/*.JPG").size());
    }
}
