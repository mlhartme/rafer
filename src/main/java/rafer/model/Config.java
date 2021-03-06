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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static Config load(World world) throws IOException {
        return load(world.getHome().join(".rafer.json"));
    }

    public static Config load(FileNode file) throws IOException {
        World world;
        JsonObject root;
        FileNode old;
        Card card;
        List<Volume> volumes;
        FileNode gpxTracks;
        FileNode inboxTrash;

        world = file.getWorld();
        old = world.getWorking();
        world.setWorking(world.getHome());
        try {
            root = new JsonParser().parse(file.readString()).getAsJsonObject();
            card = new Card(dir(world, root, "card"), "DCIM");
            volumes = volumes(world, root, "volumes");
            gpxTracks = dir(world, root, "gpxTracks");
            inboxTrash = dir(world, root, "inboxTrash");
            return new Config(card, volumes, gpxTracks, inboxTrash);
        } finally {
            world.setWorking(old);
        }
    }


    //--

    private static List<Volume> volumes(World world, JsonObject root, String name) throws IOException {
        List<Volume> result;
        JsonObject v;

        result = new ArrayList<>();
        for (JsonElement element : field(root, name).getAsJsonArray()) {
            v = element.getAsJsonObject();
            result.add(volume(world, v));
        }
        return result;
    }

    private static Volume volume(World world, JsonObject volume) throws IOException {
        String name;
        FileNode directory;

        name = string(volume, "name");
        directory = dir(world, volume, "directory");
        return new Volume(name, directory);
    }

    private static FileNode dir(World world, JsonObject root, String name) throws IOException {
        return world.file(string(root, name));
    }

    private static String string(JsonObject root, String name) throws IOException {
        return field(root, name).getAsString();
    }

    private static JsonElement field(JsonObject root, String name) throws IOException {
        JsonElement result;

        result = root.get(name);
        if (result == null) {
            throw new IOException("missing field: " + name);
        }
        return result;
    }

    //--

    public final Card card;
    public final List<Volume> volumes;
    public final FileNode gpxTracks;
    public final FileNode inboxTrash;


    public Config(Card card, List<Volume> volumes, FileNode gpxTracks, FileNode inboxTrash) {
        this.card = card;
        this.volumes = volumes;
        this.gpxTracks = gpxTracks;
        this.inboxTrash = inboxTrash;
    }

    public Volume lookup(String name) {
        for (Volume volume: volumes) {
            if (volume.name.equals(name)) {
                return volume;
            }
        }
        return null;
    }
}
