package rafer.model;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Card {
    private final FileNode directory;
    private final String path;

    public Card(FileNode directory, String path) {
        this.directory = directory;
        this.path = path;
    }

    public boolean available() {
        return directory.isDirectory();
    }

    /**
     * @return true if files have been downloaded to dest
     */
    public boolean download(Console console, FileNode dest) throws IOException {
        List<FileNode> images;
        List<FileNode> downloaded;
        FileNode root;

        Utils.directory("card", directory);

        root = directory.join(path);
        images = findImages(root);
        downloaded = download(console, images, dest);
        onCardBackup(console, root, downloaded);
        ejectOpt(console);
        return !downloaded.isEmpty();
    }

    /** @return images actually downloaded */
    private List<FileNode> download(Console console, List<FileNode> images, FileNode dest) throws IOException {
        List<FileNode> result;
        FileStore store;
        long available;
        FileNode destImage;

        store = Files.getFileStore(dest.toPath());
        console.info.println("downloading " + images.size() + " images to " + dest);
        result = new ArrayList<>(images.size());
        for (FileNode image : images) {
            available = store.getUsableSpace();
            if (available < 1024l * 1024 * 1024) {
                System.out.println("WARNING: disk space is low -- download is incomplete!");
                break;
            }
            destImage = dest.join(image.getName());
            result.add(image);
            image.copyFile(destImage);
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

    private void onCardBackup(Console console, FileNode src, List<FileNode> srcImages) throws IOException {
        FileNode downloaded;
        String path;
        FileNode destImage;

        downloaded = directory.join("DOWNLOADED");
        if (downloaded.isDirectory()) {
            console.error.println("deleting " + downloaded);
            downloaded.deleteTree();
        }
        downloaded.checkNotExists();

        console.info.println("moving " + srcImages.size() + " images from " + src + " to " + downloaded);
        for (FileNode image : srcImages) {
            path = image.getRelative(src);
            destImage = downloaded.join(path);
            destImage.getParent().mkdirsOpt();
            image.move(destImage);
        }
        if (findImages(src).isEmpty()) {
            console.info.println("complete download, removing " + src);
        }
    }

    /** @return list of RAF or JPG files */
    private static List<FileNode> findImages(FileNode root) throws IOException {
        List<FileNode> result;
        String name;

        root.checkDirectory();
        result = new ArrayList<>();
        for (FileNode other : root.find("**/*")) {
            if (other.isDirectory()) {
                // ignore
            } else if (other.getName().startsWith(".")) {
                // ignore, e.g .DS_Store, .dropbox_device, finder previews: ._*
            } else {
                name = other.getName();
                if (name.endsWith(Utils.JPG) || name.endsWith(Utils.RAF)) {
                    result.add(other);
                } else {
                    throw new IOException("unexpected file in image folder: " + other);
                }
            }
        }
        return result;
    }
}
