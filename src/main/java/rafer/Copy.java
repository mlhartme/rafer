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
import net.oneandone.sushi.fs.World;
import rafer.model.Archive;
import rafer.model.Config;
import rafer.model.Patch;
import rafer.model.Volume;

import java.io.IOException;

public class Copy {
    private final Console console;
    private final Volume master;
    private final Volume slave;

    public Copy(World world, Console console, String master, String slave) throws IOException {
        Config config;

        config = Config.load(world);
        this.console = console;
        this.master = config.lookup(master);
        if (this.master == null) {
            throw new ArgumentException("no such volume: " + master);
        }
        this.slave = config.lookup(slave);
        if (this.slave == null) {
            throw new ArgumentException("no such volume: " + slave);
        }
        if (this.master == this.slave) {
            throw new ArgumentException("cannot copy to itself");
        }
    }

    public void run() throws IOException {
        Patch patch;

        try (Archive from = master.open(); Archive to = slave.open()) {
            patch = to.diff(from);
            if (patch.isEmpty()) {
                console.info.println("nothing to do");
                return;
            }
            console.info.println(patch);
            console.readline("Press return to fix, ctrl-c to abort: ");
            patch.apply();
        }
    }
}
