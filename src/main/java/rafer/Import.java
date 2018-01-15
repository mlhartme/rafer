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
    private final Card card;
    private final boolean noGeo;

    public Import(World world, Console console, boolean noGeo, String cardOpt) throws IOException {
        super(console, true);
        this.config = rafer.model.Config.load(world);
        this.world = world;
        this.noGeo = noGeo;
        if (cardOpt.isEmpty()) {
            card = config.card;
        } else {
            card = new Card(world.file(cardOpt), "");
        }
    }

    public void doRun() throws IOException {
        FileNode tmp;
        Volume localVolume;
        Images images;

        localVolume = config.volumes.get(0);
        if (!localVolume.available()) {
            throw new IOException("local archive not available");
        }
        if (!card.available()) {
            throw new IOException("no card: " + card);
        }
        try (Archive local = localVolume.open()) {
            local.backup();
            tmp = world.getTemp().createTempDirectory();
            if (!card.download(console, tmp)) {
                console.info.println("no images");
                return;
            }
            images = Images.normalize(console, tmp);
            if (!noGeo) {
                console.info.println("adding geotags ...");
                images.geotags(console, config.gpxTracks);
            }
            console.info.println("archiving images at " + localVolume + " ...");
            images.archive(local);
            tmp.deleteTree(); // delete directory doesn't work, because JPGs still exist
        }
    }
}
