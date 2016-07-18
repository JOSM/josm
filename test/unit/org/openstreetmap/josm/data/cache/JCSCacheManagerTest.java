// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests for class {@link JCSCacheManager}.
 */
public class JCSCacheManagerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12054">Bug #12054</a>.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testLoggingAdaptor12054() throws IOException {
        JCSCacheManager.getCache("foobar", 1, 0, "foobar"); // cause logging adaptor to be initialized
        Logger.getLogger("org.apache.commons.jcs").warning("{switch:0}");
    }
}
