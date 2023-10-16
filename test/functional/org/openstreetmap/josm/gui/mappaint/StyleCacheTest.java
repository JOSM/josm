// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.Rendering;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Pair;

/**
 * Test {@link StyleCache}.
 */
@BasicPreferences
@Main
@org.openstreetmap.josm.testutils.annotations.MapPaintStyles
@Projection
@Timeout(60)
class StyleCacheTest {

    private static final int IMG_WIDTH = 1400;
    private static final int IMG_HEIGHT = 1050;

    private static Graphics2D g;
    private static BufferedImage img;
    private static NavigatableComponent nc;
    private static DataSet dsCity;
    private static DataSet dsCity2;

    @BeforeAll
    static void beforeAll() throws IllegalDataException, IOException {
        try (InputStream in = Compression.getUncompressedFileInputStream(new File("nodist/data/neubrandenburg.osm.bz2"))) {
            dsCity = OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
        }
        dsCity2 = new DataSet(dsCity);
    }

    /**
     * Load the test data that is required.
     * @throws Exception If an error occurred during load.
     */
    @BeforeEach
    public void load() throws Exception {
        img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Free the memory allocated for this test.
     * <p>
     * Since we are running junit in non-forked mode, we don't know when this test will not be referenced any more.
     */
    @AfterAll
    public static void unload() {
        g = null;
        img = null;
        nc = null;
        dsCity = null;
        dsCity2 = null;
    }

    /**
     * Create the temporary graphics
     */
    @BeforeEach
    public void loadGraphicComponents() {
        g = (Graphics2D) img.getGraphics();
        g.setClip(0, 0, IMG_WIDTH, IMG_WIDTH);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, IMG_WIDTH, IMG_WIDTH);
        nc = new MapView(MainApplication.getLayerManager(), null);
        nc.setBounds(0, 0, IMG_WIDTH, IMG_HEIGHT);
    }

    /**
     * Verifies, that the intern pool is not growing when repeatedly rendering the
     * same set of primitives (and clearing the calculated styles each time).
     * <p>
     * If it grows, this is an indication that the {@code equals} and {@code hashCode}
     * implementation is broken and two identical objects are not recognized as equal
     * or produce different hash codes.
     * <p>
     * The opposite problem (different objects are mistaken as equal) has more visible
     * consequences for the user (wrong rendering on the map) and is not recognized by
     * this test.
     */
    @Test
    void testStyleCacheInternPool() {
        MapPaintStyles.getStyles().clearCached();
        StyleCache.clearStyleCachePool();
        Bounds bounds = new Bounds(53.56, 13.25, 53.57, 13.26);
        Rendering visitor = new StyledMapRenderer(g, nc, false);
        nc.zoomTo(bounds);
        Integer internPoolSize = null;
        for (int i = 0; i < 10; i++) {
            visitor.render(dsCity, true, bounds);
            MapPaintStyles.getStyles().clearCached();
            int newInternPoolSize = StyleCache.getInternPoolSize();
            if (internPoolSize == null) {
                internPoolSize = newInternPoolSize;
            } else {
                if (internPoolSize != newInternPoolSize) {
                    System.err.println("style sources:");
                    for (StyleSource s : MapPaintStyles.getStyles().getStyleSources()) {
                        System.err.println(s.url + " active:" + s.active);
                    }
                }
                assertEquals(internPoolSize.intValue(), newInternPoolSize, "intern pool size");
            }
        }
    }

    /**
     * Verifies, that the number of {@code StyleElementList} instances stored
     * for all the rendered primitives is actually low (as intended).
     * <p>
     * Two primitives with the same style should share one {@code StyleElementList}
     * instance for the cached style elements. This is verified by counting all
     * the instances using {@code A == B} identity.
     */
    @Test
    void testStyleCacheInternPool2() {
        StyleCache.clearStyleCachePool();
        Bounds bounds = new Bounds(53.56, 13.25, 53.57, 13.26);
        Rendering visitor = new StyledMapRenderer(g, nc, false);
        nc.zoomTo(bounds);
        visitor.render(dsCity2, true, bounds);

        IdentityHashMap<StyleElementList, Integer> counter = new IdentityHashMap<>();
        int noPrimitives = 0;
        for (OsmPrimitive osm : dsCity2.allPrimitives()) {
            // primitives, that have been rendered, should have the cache populated
            if (osm.getCachedStyle() != null) {
                noPrimitives++;
                Pair<StyleElementList, Range> p = osm.getCachedStyle().getWithRange(nc.getDist100Pixel(), false);
                StyleElementList sel = p.a;
                assertNotNull(sel);
                counter.merge(sel, 1, Integer::sum);
            }
        }
        int EXPECTED_NO_PRIMITIVES = 4294; // needs to be updated if data file or bbox changes
        assertEquals(EXPECTED_NO_PRIMITIVES, noPrimitives,
                "The number of rendered primitives should be " + EXPECTED_NO_PRIMITIVES);
        assertTrue(counter.size() < 100,
                "Too many StyleElementList instances, they should be shared using the StyleCache");
    }
}
