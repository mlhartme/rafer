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

import net.oneandone.inline.ArgumentException;
import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import rafer.model.Archive;
import rafer.model.Config;
import rafer.model.Patch;
import rafer.model.Volume;

import java.io.IOException;

public class Index {
    private final Console console;
    private final boolean md5;
    private final Volume volume;

    public Index(World world, Console console, boolean md5, String name) throws IOException {
        this.console = console;
        this.md5 = md5;
        this.volume = Config.load(world).lookup(name);
        if (volume == null) {
            throw new ArgumentException("no such volume: " + name);
        }
    }

    public void run() throws IOException {
        Patch patch;

        try (Archive archive = volume.open()) {
            patch = archive.verify(md5);
            if (patch.isEmpty()) {
                console.info.println("ok");
                return;
            }
            console.info.println(patch);
            console.readline("Press return to fix, ctrl-c to abort: ");
            patch.apply();
        }
    }
}
