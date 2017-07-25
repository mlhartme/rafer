package rafer.model;

import net.oneandone.sushi.fs.file.FileNode;
import rafer.Sync;

import java.io.IOException;
import java.util.Date;

/**
 * Index and directory containing image files. Uses the file get the modified date, adjusts the directory structure accordingly.
 */
public class Archive implements AutoCloseable {
    public static Archive open(FileNode directory, Date start, Date end) throws IOException {
        return new Archive(directory, Index.load(directory), start, end);
    }

    private final FileNode directory;
    private final Index index;
    private final Date start; // inclusive [
    private final Date end; // [ exclusive

    private Archive(FileNode directory, Index index, Date start, Date end) {
        this.directory = directory;
        this.index = index;
        this.start = start;
        this.end = end;
    }

    public boolean contains(Date date) {
        return start.before(date) && date.before(end);
    }

    // TODO: dont adjust file name with date before calling this method; moveInfo should change the name itself
    public void moveInto(FileNode src) throws IOException {
        Date modified;
        FileNode dest;
        String path;

        modified = Sync.getDate(src.getName());
        dest = directory.join(Utils.MONTH_FMT.format(modified), src.getName());
        dest.getParent().mkdirsOpt();
        dest.checkNotExists();
        src.move(dest); // dont copy - disk might be full
        dest.setLastModified(modified.getTime());
        path = dest.getRelative(directory);
        index.put(path, dest.md5());
    }

    /** @return patch to bring this Archive in line with orig */
    public Patch diff(Archive master) throws IOException {
        Date date;
        Patch patch;

        patch = new Patch();
        for (String path : index) {
            date = getDate(path);
            if (!master.contains(date)) {
                continue;
            }
            final String md5 = master.index.get(path);
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
                        master.directory.join(path).copyFile(directory.join(path));
                        index.put(path, md5);
                    }
                });
            }
        }
        for (String path : master.index) {
            date = getDate(path);
            if (!contains(date)) {
                continue;
            }
            final String md5 = index.get(path);
            if (md5 == null) {
                patch.add(new Action("A " + path) {
                    @Override
                    public void invoke() throws IOException {
                        FileNode dest;

                        dest = directory.join(path);
                        dest.getParent().mkdirsOpt();
                        master.directory.join(path).copyFile(dest);
                        index.put(path, master.index.get(path));
                    }
                });
            } else {
                // existing files have already been handle in the first loop above
            }
        }
        return patch;
    }

    public static Date getDate(String path) {
        return Sync.getDate(path.substring(path.lastIndexOf('/') + 1));
    }

    /** @return patch to adjust index; fill will not be changed */
    public Patch verify(boolean md5) throws IOException {
        return index.verify(directory, md5);
    }

    public int images() throws IOException {
        return Index.load(directory).size();
    }

    public FileNode getFile(String name) {
        return directory.join(Utils.MONTH_FMT.format(Sync.getDate(name)), name);
    }

    public FileNode getFile(String basename, String ext) {
        return Sync.getFile(basename, directory, ext);
    }

    // does not compare then name
    public boolean same(Archive other) {
        return index.equals(other.index) && start == other.start && end == other.end;
    }

    @Override
    public void close() throws IOException {
        index.save(directory);
    }

    public int size() {
        return index.size();
    }
}
