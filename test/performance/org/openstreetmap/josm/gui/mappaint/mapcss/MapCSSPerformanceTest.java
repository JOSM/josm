// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.fail;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.MapRendererPerformanceTest;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

/**
 * This performance test measures the time for a full run of MapPaintVisitor.visitAll()
 * against a test data set using a test style.
 *
 */
public class MapCSSPerformanceTest {

    /* ------------------------ configuration section  ---------------------------- */
    /**
     * The path to the style file used for rendering.
     */
    static final String STYLE_FILE = "resources/styles/standard/elemstyles.mapcss";

    /**
     * The data file to be rendered
     */
    static final String DATA_FILE = "nodist/data/neubrandenburg.osm.bz2";
    /* ------------------------ / configuration section  ---------------------------- */

    DataSet ds;

    static void checkTestEnvironment() {
          File f = new File(STYLE_FILE);
          if (!f.isFile() || !f.exists()) {
              fail("STYLE_FILE refers to '"+STYLE_FILE+"'. This is either not a file or doesn't exist.\n" +
                      "Please update configuration settings in the unit test file.");
          }
    }

    /**
     * Setup test.
     */
    @BeforeClass
    public static void createJOSMFixture() {
        JOSMFixture.createPerformanceTestFixture().init(true);
    }

    long timed(Runnable callable) {
        long before = System.currentTimeMillis();
        callable.run();
        long after = System.currentTimeMillis();
        return after - before;
    }

    void loadStyle() {
        System.out.print("Loading style '"+STYLE_FILE+"' ...");
        MapCSSStyleSource source = new MapCSSStyleSource(
            new SourceEntry(
                SourceType.MAP_PAINT_STYLE,
                STYLE_FILE,
                "test style",
                "a test style",
                true // active
            )
        );
        source.loadStyleSource();
        Collection<Throwable> errors = source.getErrors();
        if (!errors.isEmpty()) {
            fail("Failed to load style file ''"+STYLE_FILE+"''. Errors: "+errors);
        }
        MapRendererPerformanceTest.resetStylesToSingle(source);
        System.out.println("DONE");
    }

    void loadData() throws IllegalDataException, IOException {
        System.out.print("Loading data file '"+DATA_FILE+"' ...");
        ds = OsmReader.parseDataSet(Compression.getUncompressedFileInputStream(new File(DATA_FILE)), null);
        System.out.println("DONE");
    }

    /**
     * Measures time for style preparation.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if any invalid data is found
     */
    @Test
    public void measureTimeForStylePreparation() throws IllegalDataException, IOException {
        loadStyle();
        loadData();

        NavigatableComponent mv = new NavigatableComponent();
        mv.setBounds(0, 0, 1024, 768);
        BufferedImage img = new BufferedImage(mv.getWidth(), mv.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        g.setClip(0, 0, mv.getWidth(), mv.getHeight());
        StyledMapRenderer visitor = new StyledMapRenderer(g, mv, false);

        System.out.print("Rendering ...");
        long time = timed(
            () -> visitor.render(ds, false, new Bounds(-90, -180, 90, 180))
        );
        System.out.println("DONE");
        System.out.println("data file : "+DATA_FILE);
        System.out.println("style file: "+STYLE_FILE);
        System.out.println("");
        System.out.println("Rendering took "+time+" ms.");
    }
}
