// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.Rendering;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Test {@link StyleCache}.
 *
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
public class StyleCacheTest {

    private static final int IMG_WIDTH = 1400;
    private static final int IMG_HEIGHT = 1050;

    private static Graphics2D g;
    private static BufferedImage img;
    private static NavigatableComponent nc;
    private static DataSet dsCity;

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
    }

    @Test
    public void testStyleCacheInternPool() throws Exception {
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
}
