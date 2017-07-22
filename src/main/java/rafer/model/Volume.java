package rafer.model;

import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/** Archive Factory. */
public class Volume {
    public final String name;
    private final FileNode directory;
    private final Date start;
    private final Date end;

    // dates as YYmmDD
    public Volume(String name, FileNode directory) {
        this(name, directory, "010101", "191231");
    }

    public Volume(String name, FileNode directory, String start, String end) {
        this.name = name;
        this.directory = directory;
        try {
            this.start = Utils.LINKED_FMT.parse(start);
            this.end = Utils.LINKED_FMT.parse(end);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean available() {
        return directory.isDirectory();
    }

    public Archive open() throws IOException {
        return Archive.open(directory, start, end);
    }

    public String toString() {
        return name;
    }
}
