// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.xml.sax.InputSource;

/**
 * Unit tests of {@link Capabilities} class.
 */
public class CapabilitiesTest {

    /**
     * Unit test of {@link Capabilities}
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void testCapabilities() throws Exception {
        final Path path = Paths.get(TestUtils.getTestDataRoot(), "__files/api/0.6/capabilities");
        final Capabilities capabilities;
        try (InputStream inputStream = Files.newInputStream(path)) {
            capabilities = Capabilities.CapabilitiesParser.parse(new InputSource(inputStream));
        }
        assertEquals(10000, capabilities.getMaxChangesetSize());
        assertEquals(2000, capabilities.getMaxWayNodes());
        assertTrue(capabilities.isOnImageryBlacklist("http://mt0.google.com/vt/lyrs=p&hl=en&x={x}&y={y}&z={z}"));
        assertEquals(Collections.singletonList(".*\\.google(apis)?\\..*/(vt|kh)[\\?/].*([xyz]=.*){3}.*"), capabilities.getImageryBlacklist());
    }
}
