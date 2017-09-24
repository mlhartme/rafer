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

import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static final SimpleDateFormat LINKED_FMT = new SimpleDateFormat("yyMMdd");
    public static final SimpleDateFormat DAY_FMT = new SimpleDateFormat("yyyy/MM/dd");
    public static final SimpleDateFormat MONTH_FMT = new SimpleDateFormat("yyyy/MM");

    public static final String RAF = ".RAF";
    public static final String JPG = ".JPG";

    public static String getParent(String name) {
        return Utils.MONTH_FMT.format(Utils.getDate(name));
    }

    public static String getPath(String name) {
        return getParent(name) + "/" + name;
    }

    public static Date getDate(String name) {
        Date date;

        if (!name.startsWith("r") || name.indexOf('x') != 7) {
            throw new IllegalArgumentException("not a lined name: " + name);
        }
        try {
            date = Utils.LINKED_FMT.parse(name.substring(1, 7));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
        return date;
    }

    public static void directory(String name, FileNode dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException(name + " not found: " + dir.getAbsolute());
        }
    }

    public static FileNode jpg(FileNode raf) {
        return raf.getParent().join(Strings.removeRight(raf.getName(), Utils.RAF) + Utils.JPG);
    }
}
