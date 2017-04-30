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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Clean {
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
    private static final SimpleDateFormat FMT2 = new SimpleDateFormat("yyyy-MM-dd - hh-mm-ss");

    private static final List<String> movies = Strings.toList("AVI", "MOV", "mov", "MP4", "mp4");

    public static void main(String[] args) throws IOException, ParseException {
        World world;
        FileNode dir;
        String name;
        int idx;
        String ext;
        Set<String> extensions = new HashSet<>();

        FMT.parse("2015-09-26 19.26.28");

        world = World.create();
        //dir = world.file("/Volumes/Data/Bilder");
        dir = world.file("/Volumes/Neuerkeller/Bilder");
        int count = 0;
        for (FileNode month : dir.find("*/??")) {
            month.checkDirectory();
            for (FileNode image : month.list()) {
                count++;
                name = image.getName();
                if (name.equals("CaptureOne")) {
                    continue;
                }
                image.checkFile();
                if (image.getName().equals(".DS_Store")) {
                    continue;
                }
                idx = name.lastIndexOf('.');
                if (idx == -1) {
                    throw new IOException(name);
                }
                ext = name.substring(idx + 1);
                extensions.add(ext);
                name = name.substring(0, idx);

                if (movies.contains(ext)) {
                    // ok
                } else if (ext.equals("RAF") && name.startsWith("r") && name.charAt(7) == 'x') {
                    // ok
                } else if ("dng".equals(ext) && check(name, "DSCF")) {
                    // ok
                } else if ("dng".equals(ext) && check(name, "SAM_")) {
                    // ok
                } else if ("JPG".equals(ext) && check(name, "CIMG")) {
                    // ok
                } else if ("JPG".equals(ext) && check(name, "SAM_")) {
                    // ok
                } else if ("JPG".equals(ext) && check(name, "DSC")) {
                    // ok
                } else if ("RAF".equals(ext) && check(name, "DESC")) {
                    // ok
                } else if ("JPG".equals(ext) && check(name, "IMG_")) {
                    // ok
                } else if (isDate(name)) {
                } else if (isDate2(name)) {
                } else {
                    try {
                        FMT.parse(name);
                        // ok
                    } catch (ParseException e) {
                        System.out.println("TODO: " + image.getName());

                    }
                }
            }
        }
        System.out.println("count: " + count);
        System.out.println("extensions: " + extensions);
    }

    private static boolean isDate(String name) {
        try {
            FMT.parse(name);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private static boolean isDate2(String name) {
        try {
            FMT2.parse(name);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private static boolean check(String str, String prefix) {
        if (!str.startsWith(prefix)) {
            return false;
        }
        return digits_4_8(str);
    }

    private static boolean digits_4_8(String str) {
        if (str.length() != 8) {
            return false;
        }
        for (int i = 4; i < 8; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
