// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;

public class StyledMapRendererPerformanceTest {

    private static final int IMG_WIDTH = 1400;
    private static final int IMG_HEIGHT = 1050;

    private static Graphics2D g;
    private static BufferedImage img;
    private static NavigatableComponent nc;
    private static DataSet dsRestriction;
    private static DataSet dsMultipolygon;
    private static DataSet dsCity;

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(15*60);

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

        // TODO Test should have it's own copy of styles because change in style can influence performance
        MapPaintStyles.readFromPreferences();

        try (
            InputStream fisR = new FileInputStream("data_nodist/restriction.osm");
            InputStream fisM = new FileInputStream("data_nodist/multipolygon.osm");
            InputStream fisC = Compression.getUncompressedFileInputStream(new File("data_nodist/neubrandenburg.osm.bz2"));
        ) {
            dsRestriction = OsmReader.parseDataSet(fisR, NullProgressMonitor.INSTANCE);
            dsMultipolygon = OsmReader.parseDataSet(fisM, NullProgressMonitor.INSTANCE);
            dsCity = OsmReader.parseDataSet(fisC, NullProgressMonitor.INSTANCE);
        }
    }

    private static void test(int iterations, DataSet ds, Bounds bounds) throws Exception {
        Rendering visitor = new StyledMapRenderer(g, nc, false);
        nc.zoomTo(bounds);
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

    /** run this manually to verify that the rendering is set up properly */
    private void dumpRenderedImage() throws IOException {
        File outputfile = new File("test-neubrandenburg.png");
        ImageIO.write(img, "png", outputfile);
    }
}
