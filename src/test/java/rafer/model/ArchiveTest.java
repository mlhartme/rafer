package rafer.model;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ArchiveTest {
    private static final World WORLD = World.createMinimal();

    @Test
    public void verify() throws IOException {
        FileNode dir;
        FileNode file;
        Archive master;
        Archive slave;
        Patch patch;

        dir = WORLD.getTemp().createTempDirectory();
        file = WORLD.getTemp().createTempDirectory().join("r170612x0001.raf");
        file.writeString("abc");
        master = new Volume("master", dir).open();
        master.moveInto(file, file.getName());
        assertFalse(file.exists());
        assertTrue(master.verify(true).isEmpty());

        slave = new Volume("slave", WORLD.getTemp().createTempDirectory()).open();

        // add file
        patch = slave.diff(master);
        assertEquals("A r170612x0001.raf\n", patch.toString());
        patch.apply();
        assertEquals("abc", slave.getFile(file.getName()).readString());
        assertTrue(slave.diff(master).isEmpty());
        assertTrue(master.diff(slave).isEmpty());

        // update file
        master.getFile(file.getName()).writeString("xyz");
        master.verify(true).apply();
        patch = slave.diff(master);
        assertEquals("U r170612x0001.raf\n", patch.toString());
        patch.apply();
        assertTrue(slave.diff(master).isEmpty());
        assertTrue(master.diff(slave).isEmpty());

        assertEquals("xyz", slave.getFile(file.getName()).readString());


        // remove file
        master.getFile(file.getName()).deleteFile();
        master.verify(true).apply();
        assertEquals(0, master.size());
        patch = slave.diff(master);
        assertEquals("D r170612x0001.raf\n", patch.toString());
        patch.apply();
        assertEquals(0, slave.size());

    }
}
