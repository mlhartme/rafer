package rafer.model;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.file.FileNode;
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

    /** @return list of raf files in dest */
    public List<FileNode> download(Console console, FileNode dest) throws IOException {
        List<FileNode> cardRafs;
        List<FileNode> downloaded;
        FileNode dcim;

        Sync.directory("card", directory);

        dcim = directory.join("DCIM");
        cardRafs = Sync.findRafs(console, dcim);
        downloaded = download(console, cardRafs, dest);
        onCardBackup(console, dcim, downloaded);
        ejectOpt(console);
        return downloaded;
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
            console.info.println(directory.exec("diskutil", "eject", directory.getName()));
        } catch (IOException e) {
            e.printStackTrace(console.verbose);
            console.info.println("WARNING: eject failed");
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
        if (Sync.findRafs(console, src).isEmpty()) {
            console.info.println("complete download, removing " + src);
        }
    }

}
