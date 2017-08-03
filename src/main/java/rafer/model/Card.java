package rafer.model;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.util.Strings;
import rafer.Sync;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Card {
    private final FileNode directory;

    public Card(FileNode directory) {
        this.directory = directory;
    }

    public boolean available() {
        return directory.isDirectory();
    }

    /** Throws an error for non-pairs
     * @return true if files have been downloaded to dest
     */
    public boolean download(Console console, FileNode dest) throws IOException {
        List<FileNode> cardRafs;
        List<FileNode> downloaded;
        FileNode dcim;

        Sync.directory("card", directory);

        dcim = directory.join("DCIM");
        cardRafs = findRafs(console, dcim);
        downloaded = download(console, cardRafs, dest);
        onCardBackup(console, dcim, downloaded);
        ejectOpt(console);
        return !downloaded.isEmpty();
    }

    /** @return srcRafs actually downloaded */
    private List<FileNode> download(Console console, List<FileNode> srcRafs, FileNode dest) throws IOException {
        List<FileNode> result;
        FileNode destRaf;
        FileNode destJpg;
        FileStore store;
        long available;

        store = Files.getFileStore(dest.toPath());
        console.info.println("downloading " + srcRafs.size() + " images to " + dest);
        result = new ArrayList<>(srcRafs.size());
        for (FileNode srcRaf : srcRafs) {
            available = store.getUsableSpace();
            if (available < 1024l * 1024 * 1024) {
                System.out.println("WARNING: disk space is low -- download is incomplete!");
                break;
            }
            destRaf = dest.join(srcRaf.getName());
            destJpg = Sync.jpg(destRaf);
            result.add(srcRaf);
            srcRaf.copyFile(destRaf);
            Sync.jpg(srcRaf).copyFile(destJpg);
        }
        return result;
    }

    public void ejectOpt(Console console) {
        if (directory.getParent().getName().equals("Volumes")) {
            eject(console);
        }
    }

    public void eject(Console console) {
        try {
            console.info.println(directory.getParent().exec("diskutil", "eject", directory.getName()));
        } catch (IOException e) {
            e.printStackTrace(console.verbose);
            console.info.println("WARNING: eject failed: " + e.getMessage());
        }
    }

    private void onCardBackup(Console console, FileNode src, List<FileNode> srcRafs) throws IOException {
        FileNode downloaded;
        String path;
        FileNode destRaf;

        downloaded = directory.join("DOWNLOADED");
        if (downloaded.isDirectory()) {
            console.error.println("deleting " + downloaded);
            downloaded.deleteTree();
        }
        downloaded.checkNotExists();

        console.info.println("moving " + srcRafs.size() + " images from " + src + " to " + downloaded);
        for (FileNode srcRaf : srcRafs) {
            path = srcRaf.getRelative(src);
            destRaf = downloaded.join(path);
            destRaf.getParent().mkdirsOpt();
            srcRaf.move(destRaf);
            Sync.jpg(srcRaf).move(Sync.jpg(destRaf));
        }
        if (findRafs(console, src).isEmpty()) {
            console.info.println("complete download, removing " + src);
        }
    }

    /** @return raf nodes with jpg sidecar */
    private static List<FileNode> findRafs(Console console, FileNode dcim) throws IOException {
        List<FileNode> result;
        String name;
        Filter filter;

        dcim.checkDirectory();
        filter = dcim.getWorld().filter();
        filter.include("**/*.RAF");
        filter.exclude("**/._*.RAF");
        result = dcim.find(filter);
        for (Node raf : result) {
            if (!Sync.jpg((FileNode) raf).exists()) {
                throw new IOException("missing jpg for " + raf);
            }
        }
        for (Node other : dcim.find("**/*")) {
            if (other.isDirectory()) {
                // ignore
            } else if (other.getName().startsWith(".")) {
                // ignore, e.g .DS_Store, .dropbox_device, finder previews: ._*
            } else if (!result.contains(other)) {
                name = other.getName();
                if (name.endsWith(Utils.JPG) && result.contains(other.getParent().join(Strings.removeRight(name, Utils.JPG) + Utils.RAF))) {
                    console.verbose.println("found sidecar jpg: " + other);
                } else {
                    throw new IOException("unexpected file in image folder: " + other);
                }
            }
        }
        return result;
    }
}
