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

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import rafer.model.*;

import java.io.IOException;

public class Import extends Base {
    private final World world;
    private final rafer.model.Config config;

    public Import(World world, Console console) throws IOException {
        this(world, console, rafer.model.Config.load(world));
    }

    public Import(World world, Console console, rafer.model.Config config) {
        super(console, true);
        this.world = world;
        this.config = config;
    }

    public void doRun() throws IOException {
        Process process;
        FileNode tmp;
        Volume localVolume;
        Pairs pairs;

        localVolume = config.volumes.get(0);
        if (!localVolume.available()) {
            throw new IOException("local archive not available");
        }
        if (!config.card.available()) {
            throw new IOException("no card");
        }
        process = new ProcessBuilder("caffeinate").start();
        try (Archive local = localVolume.open()) {
            local.backup();
            tmp = world.getTemp().createTempDirectory();
            if (!config.card.download(console, tmp)) {
                console.info.println("no images");
                return;
            }
            pairs = Pairs.normalize(console, tmp);
            console.info.println("adding geotags ...");
            pairs.geotags(console, config.gpxTracks);
            console.info.println("saving rafs at " + localVolume + " ...");
            pairs.moveRafs(local);
            console.info.println("moving jpgs to smugmug inbox ...");
            pairs.smugmugInbox(config.smugmugInbox);
            tmp.deleteDirectory();
        } finally {
            process.destroy();
        }
    }
}
