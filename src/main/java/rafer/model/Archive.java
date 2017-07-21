package rafer.model;

import net.oneandone.sushi.fs.file.FileNode;
import rafer.Sync;

import java.io.IOException;
import java.util.Date;

/** Directory with an Index and Image files */
public class Archive implements AutoCloseable {
    public static Archive open(FileNode directory, Date start, Date end) throws IOException {
        return new Archive(directory, Index.load(directory), start, end);
    }

    private final FileNode directory;
    private final Index index;
    private final Date start;
    private final Date end;

    private Archive(FileNode directory, Index index, Date start, Date end) {
        this.directory = directory;
        this.index = index;
        this.start = start;
        this.end = end;
    }

    public boolean contains(Date date) {
        return start.before(date) && date.before(end);
    }

    public void moveInto(FileNode src, long modified) throws IOException {
        FileNode dest;

        dest = directory.join(Utils.MONTH_FMT.format(modified), src.getName());
        dest.getParent().mkdirsOpt();
        dest.checkNotExists();
        src.move(dest); // dont copy - disk might be full
        dest.setLastModified(modified);
        index.put(dest.getRelative(directory), dest.md5());
    }

    /** @return patch to bring this Archive in line with orig */
    public Patch diff(Archive orig) throws IOException {
        Date date;
        Patch patch;

        patch = new Patch();
        for (String path : index) {
            date = Sync.getPathDate(path);
            if (!orig.contains(date)) {
                continue;
            }
            final String md5 = orig.index.get(path);
            if (md5 == null) {
                patch.add(new Action("D " + path) {
                    @Override
                    public void invoke() throws IOException {
                        directory.join(path).deleteFile();
                        index.remove(path);
                    }
                });
            } else if (!md5.equals(index.get(path))) {
                patch.add(new Action("U " + path) {
                    @Override
                    public void invoke() throws IOException {
                        orig.directory.join(path).copyFile(directory.join(path));
                        index.put(path, md5);
                    }
                });
            }
        }
        for (String path : orig.index) {
            date = Sync.getPathDate(path);
            if (!contains(date)) {
                continue;
            }
            final String md5 = index.get(path);
            if (md5 == null) {
                patch.add(new Action("A " + path) {
                    @Override
                    public void invoke() throws IOException {
                        orig.directory.join(path).copyFile(directory.join(path));
                        index.put(path, orig.index.get(path));
                    }
                });
            } else {
                // existing files have already been handle in the first loop
            }
        }
        return patch;
    }

    /** @return patch to adjust index; fill will not be changed */
    public Patch verify(boolean md5) throws IOException {
        return index.verify(directory, md5);
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

    public FileNode getFile(String basename, String ext) {
        return Sync.getFile(basename, directory, ext);
    }

    @Override
    public void close() throws IOException {
        index.save(directory);
    }
}
