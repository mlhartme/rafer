package rafer.model;

import net.oneandone.sushi.fs.World;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ConfigTest {
    @Test
    public void test() throws IOException {
        World world;
        Config config;

        world = World.create();
        config = Config.load(world.guessProjectHome(getClass()).join("src/test/config.json"));
        assertEquals(world.getHome().join(".trash/rafer").getAbsolute(), config.inboxTrash.getAbsolute());
    }
}
