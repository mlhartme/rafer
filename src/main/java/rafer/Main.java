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

import net.oneandone.inline.Cli;
import net.oneandone.inline.Console;
import net.oneandone.inline.commands.Help;
import net.oneandone.inline.commands.PackageVersion;
import net.oneandone.inline.internal.Repository;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public final class Main {
    public static void main(String[] args) throws Exception {
        System.exit(doMain(args));
    }

    public static int doMain(String... args) throws Exception {
        Cli cli;
        Console console;
        int result;

        console = Console.create();
        try (World world = World.create()) {
            cli = new Cli(new Repository(), console::handleException);
            cli.primitive(FileNode.class, "file", world.getWorking(), world::file);
            cli.begin(world)
               .begin(console, "-v -e  { setVerbose(v) setStacktraces(e) }")
                        .addDefault(new Help(console, help()), "help")
                        .add(PackageVersion.class, "version")
                        .add(Import.class, "import")
                        .add(Smugmug.class, "smugmug")
                        .add(Status.class, "status")
                        .add(Sync.class, "sync master slave")
                        .add(Index.class, "index -md5 dir");
            try {
                result = cli.run(args);
                return result;
            } finally {
                console.info.flush();
            }
        }
    }

    private static String help() throws IOException {
        StringBuilder builder;

        builder = new StringBuilder();
        builder.append("usage: 'rafer' command ...\n"
                + "   fuji raf file maintenance\n"
                + "\n"
                + "commands\n"
                + "  'status'               print status of all volumes\n"
                + "  'index' volume         create or update volume index; this is kind of a commit command\n"
                + "  'sync' master slave    sync changes from master to slave\n"
                + "  'import'               import card into local and smugmug inbox\n"
                + "  'smugmug'              smugmug upload\n"
        );
        return builder.toString();
    }

    private Main() {
    }
}
