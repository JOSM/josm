// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests for class {@link Version}.
 */
class VersionTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link Version#getAgentString}
     */
    @Test
    void testGetAgentString() {
        Version version = new Version();
        version.initFromRevisionInfo(null);
        String v = version.getAgentString(false);
        assertTrue(v.matches("JOSM/1\\.5 \\(UNKNOWN en\\)"), v);
        v = version.getAgentString(true);
        assertTrue(v.matches("JOSM/1\\.5 \\(UNKNOWN en\\).*"), v);
    }

    /**
     * Unit test of {@link Version#initFromRevisionInfo} - null case.
     */
    @Test
    void testInitFromRevisionInfoNull() {
        Version v = new Version();
        v.initFromRevisionInfo(null);
        assertEquals(Version.JOSM_UNKNOWN_VERSION, v.getVersion());
    }

    /**
     * Unit test of {@link Version#initFromRevisionInfo} - local build.
     */
    @Test
    void testInitFromRevisionInfoLocal() {
        Version v = new Version();
        v.initFromRevisionInfo(new ByteArrayInputStream(("\n" +
            "Revision: 11885\n" +
            "Is-Local-Build: true\n" +
            "Build-Date: 2017-04-12 02:08:29\n"
                ).getBytes(StandardCharsets.UTF_8)));
        assertEquals(11885, v.getVersion());
        assertEquals("11885", v.getVersionString());
        assertTrue(v.isLocalBuild());
    }
}
