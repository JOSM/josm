// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.IdentityHashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.Rendering;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.tools.Pair;

/**
 * Test {@link StyleCache}.
 */
public class StyleCacheTest {

    private static final int IMG_WIDTH = 1400;
    private static final int IMG_HEIGHT = 1050;

    private static Graphics2D g;
    private static BufferedImage img;
    private static NavigatableComponent nc;
    private static DataSet dsCity;
    private static DataSet dsCity2;

    @BeforeClass
    public static void load() throws Exception {
        JOSMFixture.createPerformanceTestFixture().init(true);
        img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) img.getGraphics();
        g.setClip(0, 0, IMG_WIDTH, IMG_WIDTH);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, IMG_WIDTH, IMG_WIDTH);
        nc = Main.map.mapView;
        nc.setBounds(0, 0, IMG_WIDTH, IMG_HEIGHT);

        MapPaintStyles.readFromPreferences();
        try (
            InputStream fisC = Compression.getUncompressedFileInputStream(new File("data_nodist/neubrandenburg.osm.bz2"));
        ) {
            dsCity = OsmReader.parseDataSet(fisC, NullProgressMonitor.INSTANCE);
        }
        try (
            InputStream fisC = Compression.getUncompressedFileInputStream(new File("data_nodist/neubrandenburg.osm.bz2"));
        ) {
            dsCity2 = OsmReader.parseDataSet(fisC, NullProgressMonitor.INSTANCE);
        }
    }

    /**
     * Verifies, that the intern pool is not growing when repeatedly rendering the
     * same set of primitives (and clearing the calculated styles each time).
     *
     * If it grows, this is an indication that the {@code equals} and {@code hashCode}
     * implementation is broken and two identical objects are not recognized as equal
     * or produce different hash codes.
     *
     * The opposite problem (different objects are mistaken as equal) has more visible
     * consequences for the user (wrong rendering on the map) and is not recognized by
     * this test.
     */
    @Test
    public void testStyleCacheInternPool() {
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
                assertEquals("intern pool size", internPoolSize.intValue(), newInternPoolSize);
            }
        }
    }

    /**
     * Verifies, that the number of {@code StyleElementList} instances stored
     * for all the rendered primitives is actually low (as intended).
     *
     * Two primitives with the same style should share one {@code StyleElementList}
     * instance for the cached style elements. This is verified by counting all
     * the instances using {@code A == B} identity.
     */
    @Test
    public void testStyleCacheInternPool2() {
        Bounds bounds = new Bounds(53.56, 13.25, 53.57, 13.26);
        Rendering visitor = new StyledMapRenderer(g, nc, false);
        nc.zoomTo(bounds);
        visitor.render(dsCity2, true, bounds);

        IdentityHashMap<StyleElementList, Integer> counter = new IdentityHashMap<>();
        int noPrimitives = 0;
        for (OsmPrimitive osm : dsCity2.allPrimitives()) {
            // primitives, that have been rendered, should have the cache populated
            if (osm.mappaintStyle != null) {
                noPrimitives++;
                Pair<StyleElementList, Range> p = osm.mappaintStyle.getWithRange(nc.getDist100Pixel(), false);
                StyleElementList sel = p.a;
                Assert.assertNotNull(sel);
                Integer k = counter.get(sel);
                if (k == null) {
                    k = 0;
                }
                counter.put(sel, k + 1);
            }
        }
        int EXPECTED_NO_PRIMITIVES = 4294; // needs to be updated if data file or bbox changes
        Assert.assertEquals(
                "The number of rendered primitives should be " + EXPECTED_NO_PRIMITIVES,
                EXPECTED_NO_PRIMITIVES, noPrimitives);
        Assert.assertTrue(
                "Too many StyleElementList instances, they should be shared using the StyleCache",
                counter.size() < 100);
    }
}
