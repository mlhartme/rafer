package rafer.model;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import rafer.Sync;

import java.io.IOException;
import java.util.Date;

/** Directory with an Index and Image files */
public class Archive {
    public final String name;
    private final FileNode directory;

    public Archive(String name, FileNode directory) {
        this.name = name;
        this.directory = directory;
    }

    public boolean available() {
        return directory.isDirectory();
    }

    public void verify(Console console, boolean md5) throws IOException {
        Index old;
        Index current;

        directory.checkDirectory();
        old = Index.load(directory);
        current = old.verify(directory, md5, console);
        if (current.equals(old)) {
            console.info.println("ok: " + directory);
        } else {
            console.readline("press return to update index, ctrl-c to abort");
            current.save(directory);
        }
        console.info.println("files: " + current.size());
        console.info.println("extensions: " + current.extensions());
    }

    public int add(Console console, FileNode srcRoot) throws IOException {
        Index index;
        FileNode dest;
        Date date;
        int errors;

        index = Index.load(directory);
        errors = 0;
        for (Node src : srcRoot.find("**/*" + Utils.RAF)) {
            String path = src.getRelative(srcRoot);
            if (!index.contains(path)) {
                dest = directory.join(path);
                dest.getParent().mkdirsOpt();
                console.info.println("A " + directory.getName() + "/" + path);
                try {
                    src.copyFile(dest);
                    dest.setLastModified(src.getLastModified());
                    index.put(path, src.md5());
                } catch (IOException e) {
                    e.printStackTrace();
                    errors++;
                }
            }
        }

        for (String path : index) {
            FileNode src = srcRoot.join(path);
            if (!src.exists()) {
                try {
                    date = Sync.getDate(src.getName());
                } catch (IllegalArgumentException e) {
                    // console.info.println(src.getName());
                    // e.printStackTrace();
                    // errors++;
                    continue;
                }
                if (date.before(Sync.START_DATE)) {
                    // skip
                } else {
                    console.info.println("D " + directory.getName() + "/" + path);
                    try {
                        trash(directory, directory.join(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        errors++;
                    }
                    index.remove(path);
                }
            }
        }
        index.save(directory);
        return errors;
    }

    private void trash(FileNode root, FileNode file) throws IOException {
        FileNode dir;

        dir = root.getParent().join(".trash." + root.getName());
        dir.mkdirOpt();
        dir = dir.join(Utils.STARTED);
        dir.mkdirOpt();
        file.move(dir.join(file.getName()));
    }

    public int images() throws IOException {
        return Index.load(directory).size();
    }
}
