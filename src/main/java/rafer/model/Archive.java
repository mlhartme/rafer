package rafer.model;

import net.oneandone.sushi.fs.file.FileNode;
import rafer.Sync;

import java.io.IOException;
import java.util.Date;

/**
 * Index and directory containing files.
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

    // TODO: don't adjust file name with date before calling this method; moveInfo should change the name itself
    public FileNode moveInto(FileNode src, String path) throws IOException {
        FileNode dest;

        dest = directory.join(path);
        dest.getParent().mkdirsOpt();
        if (dest.exists()) {
            // TODO
            if (src.md5().equals(dest.md5())) {
                System.out.println("overwriting same file: " + dest);
                dest.deleteFile();
            } else {
                dest = dest.getParent().join(dest.getName() + ".2");
                System.out.println(dest + " exists");
            }
        }
        long mod = src.getLastModified();
        src.move(dest); // don't copy - disk might be full
        if (dest.getLastModified() != mod) {
            throw new IllegalStateException("move modified the file");
        }
        index.put(path, dest.md5());
        return dest;
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

    public FileNode getFile(String path) {
        return directory.join(path);
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
