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
package rafer.model;

import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public final Card card;

    // where to store rafs
    public final FileNode rafs;
    public final List<Archive> backups;
    public final FileNode gpxTracks;

    // smugmug index or null to disable smugmug sync
    public final FileNode smugmug;
    public final FileNode inboxTrash;


    public Config(World world) throws MkdirException {
        card = new Card(world.file("/Volumes/UNTITLED"));
        rafs = world.getHome().join("Pictures/Rafer");
        smugmug = world.getHome().join("Pictures/smugmug.idx");
        backups = new ArrayList<>();
        backups.add(new Archive(world.file("/Volumes/Data/Bilder")));
        backups.add(new Archive(world.file("/Volumes/Neuerkeller/Bilder")));
        gpxTracks = world.getHome().join("Dropbox/Apps/Geotag Photos Pro (iOS)");
        inboxTrash = world.getHome().join(".trash/rafer").mkdirOpt();
    }
}
