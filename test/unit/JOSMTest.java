// License: GPL. For details, see LICENSE file.
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests of {@link JOSM} class.
 */
public class JOSMTest {

    /**
     * Unit test of {@link JOSM} constructor.
     */
    @Test
    public void testJOSM() {
        assertNotNull(new JOSM());
    }
}
