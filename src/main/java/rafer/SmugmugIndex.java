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

import net.mlhartme.smuggler.cache.FolderData;
import net.mlhartme.smuggler.cli.Config;
import net.mlhartme.smuggler.smugmug.Account;
import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class SmugmugIndex extends Base {
    private final World world;
    private final rafer.model.Config config;

    public SmugmugIndex(World world, Console console) throws IOException {
        this(world, console, rafer.model.Config.load(world));
    }

    public SmugmugIndex(World world, Console console, rafer.model.Config config) {
        super(console, true);
        this.world = world;
        this.config = config;
    }

    public void doRun() throws IOException {
        Config smc;
        Account smugmugAccount;
        FolderData data;
        String str;
        FileNode file;
        FileNode prev;

        if (config.smugmug == null) {
            throw new IOException("smugmug not configured");
        }
        config.smugmug.checkFile();
        smc = Config.load(world);
        smugmugAccount = smc.newSmugmug(world);
        data = FolderData.load(smugmugAccount.user(smc.user).folder().lookupFolder(smc.folder), true);
        data.sort();
        str = data.toString();
        file = config.smugmug;
        if (file.exists()) {
            prev = file.getParent().join(file.getName() + ".bak");
            prev.deleteFileOpt();
            file.move(prev);
        }
        file.writeString(str);
    }
}
