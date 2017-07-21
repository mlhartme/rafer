package rafer.model;

import net.oneandone.sushi.fs.file.FileNode;

/** Archive Factory. */
public class Volume {
    public final String name;
    private final FileNode directory;

    public Volume(String name, FileNode directory) {
        this.name = name;
        this.directory = directory;
    }

    public boolean available() {
        return directory.isDirectory();
    }

    public Archive open() {
        return new Archive(this, directory);
    }

    public String toString() {
        return name;
    }
}
