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
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Diff;

import java.io.IOException;

public class Verify {
    private final Console console;
    private final FileNode dir;

    public Verify(Console console, FileNode dir) throws MkdirException {
        this.console = console;
        this.dir = dir;
    }

    public void run() throws IOException {
        Index old;
        Index current;
        String diff;

        dir.checkDirectory();
        old = Index.load(dir);
        current = Index.scan(dir);

        diff = Diff.diff(old.toString(), current.toString());
        if (diff.isEmpty()) {
            console.info.println("ok: " + dir);
            return;
        }
        console.info.println(diff);
        console.readline("press return to update index, ctrl-c to abort");
        current.save(dir);
    }
}
