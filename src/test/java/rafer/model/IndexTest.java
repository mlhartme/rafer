package rafer.model;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class IndexTest {
    private static final World WORLD = World.createMinimal();

    @Test
    public void verify() throws IOException {
        Index index;
        FileNode dir;
        String name;
        FileNode file;
        Patch patch;
        String md5;


        // empty index
        index = new Index();
        dir = WORLD.getTemp().createTempDirectory();
        assertTrue(index.verify(dir, true).isEmpty());

        // patch missing file
        name = "abc.jpg";
        file = dir.join(name);
        file.writeString("a");
        patch = index.verify(dir, true);
        assertEquals("A " + name + "\n", patch.toString());
        assertEquals(1, patch.size());
        patch.apply();
        assertEquals(1, index.size());
        md5 = index.get(name);
        assertNotNull("AA", md5);

        // patch broken md5
        file.writeString("b");
        assertTrue(index.verify(dir, false).isEmpty());
        patch = index.verify(dir, true);
        assertEquals("U " + name + "\n", patch.toString());
        assertEquals(1, patch.size());
        patch.apply();
        assertTrue(index.contains(name));
        assertNotEquals(md5, index.get(name));

        // patch superflous index entry
        file.deleteFile();
        patch = index.verify(dir, true);
        assertEquals(1, patch.size());
        assertEquals("D " + name + "\n", patch.toString());
        patch.apply();
        assertEquals(0, index.size());
    }
}
