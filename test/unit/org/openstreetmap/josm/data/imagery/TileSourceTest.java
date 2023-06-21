// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;

/**
 * Interface for tests common between {@link org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource} classes.
 */
public interface TileSourceTest {
    /**
     * Get a generic test info for "general" tests. <i>This will be modified!</i>
     * @return The info to use for testing
     */
    ImageryInfo getInfo();

    /**
     * Get the tile source for a test
     * @param info The info to use
     * @return The tilesource
     */
    TemplatedTileSource getTileSource(ImageryInfo info);

    /**
     * Ensure that custom headers are set
     */
    @Test
    default void testCustomHeaders() {
        final ImageryInfo info = getInfo();
        info.setCustomHttpHeaders(Collections.singletonMap("test_header", "is here"));
        final TemplatedTileSource tileSource = getTileSource(info);
        assertEquals("is here", tileSource.getHeaders().get("test_header"));
    }
}
