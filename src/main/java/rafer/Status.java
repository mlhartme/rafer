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
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.World;
import rafer.model.Archive;
import rafer.model.Volume;

import java.io.IOException;

public class Status {
    private final World world;
    private final Console console;
    private final rafer.model.Config config;

    public Status(World world, Console console) throws MkdirException {
        this(world, console, new rafer.model.Config(world));
    }

    public Status(World world, Console console, rafer.model.Config config) {
        this.world = world;
        this.console = console;
        this.config = config;
    }

    public void run() throws IOException {
        String status;

        for (Volume volume : config.backups) {
            if (volume.available()) {
                try (Archive archive = volume.open()) {
                    status = Integer.toString(archive.images());
                }
            } else {
                status = "(not available)";
            }
            console.info.println(volume + ": " + status);
        }
    }
}