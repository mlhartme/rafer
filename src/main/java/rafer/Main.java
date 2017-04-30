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
                        .add(Sync.class, "sync")
                        .add(Verify.class, "verify dir");
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
        builder.append("usage: 'fault' command ...\n"
                + "   stores secret project files in vault\n"
                + "\n"
                + "account commands\n"
                + "  'account-request' ['-batch]    creates an account request\n"
                + "  'auth'                         creates a token\n"
                + "  'passphrase'                   changes the passphrase on the account file\n"
                + "user commands\n"
                + "  'run' ['-secrets' path] ['@'project] cmd*\n"
                + "       creates the specified secrets directory (default: target/.fault if target exists; otherwise a tmp directory),\n"
                + "       populates it with the secrets of the specified project (default: current project),\n"
                + "       and executes cmd (default: bash) in the current directory\n"
                + "       with the FAULT_SECRETS environment variable pointing to the secrets directory\n"
                + "  'edit' ['@'project] cmd*\n"
                + "       same as 'run', but cmd is executed in the secrets directory (which is always a tmp directory);\n"
                + "       after cmd terminated, changes will be displayed and you can choose to store them\n"
                + "  'ssh' host cmd*\n"
                + "       logs in on the specified host and executes the specified command (default: bash --login) with the\n"
                + "       FAULT_TOKEN environment variable set\n"
                + "admin commands\n"
                + "  'init'                         initialize vault server to be used by fault; prerequisite for all other commands\n"
                + "  'status'                       list (requested) accounts and projects\n"
                + "  'config'                       edit configuration and apply changes\n"
                + "  'account-add' ['-batch] login  turns account requests into accounts\n"
                + "  'account-remove' login         removes the specified account\n"
                + "environment variables\n"
                + "  'FAULT_ADDR                    where to locate fault data on vault server, default is $VAULT_ADDR/secret/fault\n"
                + "  'VAULT_ADDR                    where to locate vault server; unused if FAULT_ADDR is set\n"
                + "files\n"
                + "   ~/.fault/account              account file\n"
                + "   ~/.fault/token                token file\n"
        );
        return builder.toString();
    }

    private Main() {
    }
}
