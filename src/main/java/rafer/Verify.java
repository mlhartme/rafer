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
import net.oneandone.sushi.fs.file.FileNode;
import rafer.model.Index;

import java.io.IOException;

public class Verify {
    private final Console console;
    private final boolean md5;
    private final FileNode dir;

    public Verify(Console console, boolean md5, FileNode dir) throws MkdirException {
        this.console = console;
        this.md5 = md5;
        this.dir = dir;
    }

    public void run() throws IOException {
        Index old;
        Index current;

        dir.checkDirectory();
        old = Index.load(dir);
        current = old.verify(dir, md5, console);
        if (current.equals(old)) {
            console.info.println("ok: " + dir);
        } else {
            console.readline("press return to update index, ctrl-c to abort");
            current.save(dir);
        }
        console.info.println("files: " + current.size());
        console.info.println("extensions: " + current.extensions());
    }
}
