// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Abstract superclass of {@code StyledMapRendererPerformanceTest} and {@code WireframeMapRendererPerformanceTest}.
 */
public abstract class AbstractMapRendererPerformanceTestParent {

    private static final int IMG_WIDTH = 1400;
    private static final int IMG_HEIGHT = 1050;

    @SuppressFBWarnings(value = "MS_PKGPROTECT")
    protected static Graphics2D g;
    @SuppressFBWarnings(value = "MS_PKGPROTECT")
    protected static BufferedImage img;
    @SuppressFBWarnings(value = "MS_PKGPROTECT")
    protected static NavigatableComponent nc;
    private static DataSet dsRestriction;
    private static DataSet dsMultipolygon;
    private static DataSet dsOverpass;
    private static DataSet dsCity;

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public Timeout globalTimeout = Timeout.seconds(15*60);

    protected static void load() throws Exception {
        JOSMFixture.createPerformanceTestFixture().init(true);
        img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) img.getGraphics();
        g.setClip(0, 0, IMG_WIDTH, IMG_HEIGHT);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
        nc = new NavigatableComponent() {
            {
                setBounds(0, 0, IMG_WIDTH, IMG_HEIGHT);
                updateLocationState();
            }

            @Override
            protected boolean isVisibleOnScreen() {
                return true;
            }

            @Override
            public Point getLocationOnScreen() {
                return new Point(0, 0);
            }
        };

        // Force reset of preferences
        StyledMapRenderer.PREFERENCE_ANTIALIASING_USE.put(true);
        StyledMapRenderer.PREFERENCE_TEXT_ANTIALIASING.put("gasp");

        try (InputStream fisR = new FileInputStream("data_nodist/restriction.osm");
                InputStream fisM = new FileInputStream("data_nodist/multipolygon.osm");
                InputStream fisC = Compression.getUncompressedFileInputStream(new File("data_nodist/neubrandenburg.osm.bz2"));
                InputStream fisO = Compression.getUncompressedFileInputStream(new File("data_nodist/overpass-download.osm.bz2"));) {
            dsRestriction = OsmReader.parseDataSet(fisR, NullProgressMonitor.INSTANCE);
            dsMultipolygon = OsmReader.parseDataSet(fisM, NullProgressMonitor.INSTANCE);
            dsCity = OsmReader.parseDataSet(fisC, NullProgressMonitor.INSTANCE);
            dsOverpass = OsmReader.parseDataSet(fisO, NullProgressMonitor.INSTANCE);
        }
    }

    protected static void clean() throws Exception {
        g = null;
        img = null;
        nc = null;
        dsRestriction = null;
        dsMultipolygon = null;
        dsCity = null;
    }

    protected abstract Rendering buildRenderer();

    protected final void test(int iterations, DataSet ds, Bounds bounds) throws Exception {
        nc.zoomTo(bounds);
        Rendering visitor = buildRenderer();
        for (int i = 0; i < iterations; i++) {
            visitor.render(ds, true, bounds);
        }
    }

    @Test
    public void testRestriction() throws Exception {
        test(700, dsRestriction, new Bounds(51.12, 14.147472381591795, 51.128, 14.162492752075195));
    }

    @Test
    public void testRestrictionSmall() throws Exception {
        test(1500, dsRestriction, new Bounds(51.125, 14.147, 51.128, 14.152));
    }

    @Test
    public void testMultipolygon() throws Exception {
        test(400, dsMultipolygon, new Bounds(60, -180, 85, -122));
    }

    @Test
    public void testMultipolygonSmall() throws Exception {
        test(850, dsMultipolygon, new Bounds(-90, -180, 90, 180));
    }

    @Test
    /**
     * Complex polygon (Lake Ontario) with small download area.
     */
    public void testOverpassDownload() throws Exception {
        test(20, dsOverpass, new Bounds(43.4510496, -76.536684, 43.4643202, -76.4954853));
    }

    @Test
    public void testCity() throws Exception {
        test(50, dsCity, new Bounds(53.51, 13.20, 53.59, 13.34));
    }

    @Test
    public void testCitySmall() throws Exception {
        test(70, dsCity, new Bounds(52, 11, 55, 14));
    }

    @Test
    public void testCityPart1() throws Exception {
        test(250, dsCity, new Bounds(53.56, 13.25, 53.57, 13.26));
    }

    @Test
    public void testCityPart2() throws Exception {
        test(200, dsCity, new Bounds(53.55, 13.29, 53.57, 13.30));
    }

    @Test
    public void testCitySmallPart2() throws Exception {
        test(200, dsCity, new Bounds(53.56, 13.295, 53.57, 13.30));
    }

    /**
     * run this manually to verify that the rendering is set up properly
     * @throws IOException if any I/O error occurs
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD")
    private void dumpRenderedImage() throws IOException {
        ImageIO.write(img, "png", new File("test-neubrandenburg.png"));
    }
}
