// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.TileJobOptions;

/**
 * Test class for {@link MVTTile}
 */
class MVTTileTest {
    private static ICacheAccess<String, BufferedImageCacheEntry> cache;
    private MapboxVectorTileSource tileSource;
    private MapboxVectorCachedTileLoader loader;

    @BeforeAll
    static void classSetup() {
        cache = JCSCacheManager.getCache("testMapillaryCache");
    }

    @AfterAll
    static void classTearDown() {
        cache.clear();
        cache = null;
    }

    @BeforeEach
    void setup() {
        cache.clear();
        tileSource = new MapboxVectorTileSource(new ImageryInfo("Test Mapillary", "file:/" + TestUtils.getTestDataRoot()
          + "pbf/mapillary/{z}/{x}/{y}.mvt"));
        final TileJobOptions options = new TileJobOptions(1, 1, Collections.emptyMap(), 3600);
        loader = new MapboxVectorCachedTileLoader(null, cache, options);
    }

    /**
     * Provide arguments for {@link #testMVTTile(BufferedImage, Boolean)}
     * @return The arguments to use
     */
    private static Stream<Arguments> testMVTTile() {
        return Stream.of(
          Arguments.of(null, Boolean.TRUE),
          Arguments.of(Tile.LOADING_IMAGE, Boolean.TRUE),
          Arguments.of(Tile.ERROR_IMAGE, Boolean.TRUE),
          Arguments.of(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY), Boolean.FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource("testMVTTile")
    void testMVTTile(BufferedImage image, Boolean isLoaded) {
        MVTTile tile = new MVTTile(tileSource, 3249, 6258, 14);
        tile.setImage(image);
        assertEquals(image, tile.getImage());

        TileJob job = loader.createTileLoaderJob(tile);
        // Ensure that we are not getting a cached tile
        job.submit(true);
        Awaitility.await().atMost(Durations.ONE_SECOND).until(tile::isLoaded);
        if (isLoaded) {
            Awaitility.await().atMost(Durations.ONE_SECOND).until(() -> tile.getImage() == MVTTile.CLEAR_LOADED);
            assertEquals(2, tile.getLayers().size());
            assertEquals(4096, tile.getExtent());
            // Ensure that we have the clear image set, such that the tile doesn't add to the dataset again
            // and we don't have a loading image
            assertEquals(MVTTile.CLEAR_LOADED, tile.getImage());
        } else {
            assertNull(tile.getLayers());
            assertEquals(image, tile.getImage());
        }
    }

}
