// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MVTTile;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorCachedTileLoader;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.MapboxVectorTileSource;
import org.openstreetmap.josm.gui.layer.imagery.MVTLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test for {@link VectorDataSet}
 */
class VectorDataSetTest {
    /**
     * Make some methods available for this test class
     */
    private static class MVTLayerMock extends MVTLayer {
        private final Collection<MVTTile> finishedLoading = new HashSet<>();

        MVTLayerMock(ImageryInfo info) {
            super(info);
        }

        @Override
        protected MapboxVectorTileSource getTileSource() {
            return super.getTileSource();
        }

        protected MapboxVectorCachedTileLoader getTileLoader() {
            if (this.tileLoader == null) {
                this.tileLoader = this.getTileLoaderFactory().makeTileLoader(this, Collections.emptyMap(), 7200);
            }
            if (this.tileLoader instanceof MapboxVectorCachedTileLoader) {
                return (MapboxVectorCachedTileLoader) this.tileLoader;
            }
            return null;
        }

        @Override
        public void finishedLoading(MVTTile tile) {
            super.finishedLoading(tile);
            this.finishedLoading.add(tile);
        }

        public Collection<MVTTile> finishedLoading() {
            return this.finishedLoading;
        }
    }

    @RegisterExtension
    JOSMTestRules rule = new JOSMTestRules().projection();

    /**
     * Load arbitrary tiles
     * @param layer The layer to add the tiles to
     * @param tiles The tiles to load ([z, x, y, z, x, y, ...]) -- must be divisible by three
     */
    private static void loadTile(MVTLayerMock layer, int... tiles) {
        if (tiles.length % 3 != 0 || tiles.length == 0) {
            throw new IllegalArgumentException("Tiles come with a {z}, {x}, and {y} component");
        }
        final MapboxVectorTileSource tileSource = layer.getTileSource();
        MapboxVectorCachedTileLoader tileLoader = layer.getTileLoader();
        Collection<MVTTile> tilesCollection = new ArrayList<>();
        for (int i = 0; i < tiles.length / 3; i++) {
            final MVTTile tile = (MVTTile) layer.createTile(tileSource, tiles[3 * i + 1], tiles[3 * i + 2], tiles[3 * i]);
            tileLoader.createTileLoaderJob(tile).submit();
            tilesCollection.add(tile);
        }
        Awaitility.await().atMost(Durations.FIVE_SECONDS).until(() -> layer.finishedLoading().size() == tilesCollection
          .size());
    }

    private MVTLayerMock layer;

    @BeforeEach
    void setup() {
        // Create the preconditions for the test
        final ImageryInfo info = new ImageryInfo();
        info.setName("en", "Test info");
        info.setUrl("file:/" + Paths.get(TestUtils.getTestDataRoot(), "pbf", "mapillary", "{z}", "{x}", "{y}.mvt"));
        layer = new MVTLayerMock(info);
    }

    @Test
    void testNodeDeduplication() {
        final VectorDataSet dataSet = this.layer.getData();
        assertTrue(dataSet.allPrimitives().isEmpty());

        // Set the zoom to 14, as that is the tile we are checking
        dataSet.setZoom(14);
        loadTile(this.layer, 14, 3248, 6258);

        // Actual test
        // With Mapillary, only ends of ways should be untagged
        // There are 55 actual "nodes" in the data with two nodes for the ends of the way.
        // One of the end nodes is a duplicate of an actual node.
        assertEquals(56, dataSet.getNodes().size());
        // There should be 55 nodes from the mapillary-images layer
        assertEquals(55, dataSet.getNodes().stream().filter(node -> "mapillary-images".equals(node.getLayer())).count());
        // Please note that this dataset originally had the <i>same</i> id for all the images
        // (MVT v2 explicitly said that ids had to be unique in a layer, MVT v1 did not)
        // This number is from the 56 nodes - original node with id - single node on mapillary-sequences layer = 54
        assertEquals(54, dataSet.getNodes().stream().filter(node -> node.hasKey("original_id")).count());
        assertEquals(1, dataSet.getNodes().stream().filter(node -> node.hasKey("original_id")).map(node -> node.get("original_id"))
            .distinct().count());
        assertEquals(1, dataSet.getWays().size());
        assertEquals(0, dataSet.getRelations().size());
    }
}
