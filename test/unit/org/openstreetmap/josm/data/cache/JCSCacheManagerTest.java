// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

public class JCSCacheManagerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testLoggingAdaptor12054() throws Exception {
        JCSCacheManager.getCache("foobar", 1, 0, "foobar"); // cause logging adaptor to be initialized
        Logger.getLogger("org.apache.commons.jcs").warning("{switch:0}");
    }
}
