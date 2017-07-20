package rafer.model;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import rafer.Sync;

import java.io.IOException;
import java.util.Date;

public class Archive {
    private final FileNode destRoot;

    public Archive(FileNode destRoot) {
        this.destRoot = destRoot;
    }

    public boolean available() {
        return destRoot.isDirectory();
    }

    public int add(Console console, FileNode srcRoot) throws IOException {
        Index index;
        FileNode dest;
        Date date;
        int errors;

        index = Index.load(destRoot);
        errors = 0;
        for (Node src : srcRoot.find("**/*" + Utils.RAF)) {
            String path = src.getRelative(srcRoot);
            if (!index.contains(path)) {
                dest = destRoot.join(path);
                dest.getParent().mkdirsOpt();
                console.info.println("A " + destRoot.getName() + "/" + path);
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
                    console.info.println("D " + destRoot.getName() + "/" + path);
                    try {
                        trash(destRoot, destRoot.join(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        errors++;
                    }
                    index.remove(path);
                }
            }
        }
        index.save(destRoot);
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

}
