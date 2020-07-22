// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.CustomMatchers.hasSize;
import static org.CustomMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Unit tests of {@code UnconnectedWays} class.
 */
public class UnconnectedWaysTest {

    private UnconnectedWays bib;

    /**
     * Setup test.
     * @throws Exception if the test cannot be initialized
     */
    @Before
    public void setUp() throws Exception {
        bib = new UnconnectedWays.UnconnectedHighways();
        JOSMFixture.createUnitTestFixture().init();
        bib.initialize();
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/6313">Bug #6313</a>.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if the OSM data cannot be parsed
     * @throws FileNotFoundException if the data file cannot be found
     */
    @Test
    public void testTicket6313() throws IOException, IllegalDataException, FileNotFoundException {
        try (InputStream fis = Files.newInputStream(Paths.get("nodist/data/UnconnectedWaysTest.osm"))) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, null, null));

            bib.startTest(null);
            bib.visit(ds.allPrimitives());
            bib.endTest();
            assertThat(bib.getErrors(), isEmpty());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18051">Bug #18051</a>.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if the OSM data cannot be parsed
     * @throws FileNotFoundException if the data file cannot be found
     */
    @Test
    public void testTicket18051() throws IOException, IllegalDataException, FileNotFoundException {
        try (InputStream fis = TestUtils.getRegressionDataStream(18051, "modified-ways.osm.bz2")) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, null, null));

            bib.startTest(null);
            bib.setBeforeUpload(true);
            bib.visit(ds.allModifiedPrimitives());
            bib.endTest();
            assertThat(bib.getErrors(), isEmpty());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18136">Bug #18106</a>.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if the OSM data cannot be parsed
     * @throws FileNotFoundException if the data file cannot be found
     */
    @Test
    public void testTicket18106() throws IOException, IllegalDataException, FileNotFoundException {
        try (InputStream fis = TestUtils.getRegressionDataStream(18106, "uncon3.osm")) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, null, null));

            bib.startTest(null);
            bib.setBeforeUpload(true);
            bib.visit(ds.allPrimitives());
            bib.endTest();
            assertThat(bib.getErrors(), isEmpty());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18137">Bug #18137</a>.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if the OSM data cannot be parsed
     * @throws FileNotFoundException if the data file cannot be found
     */
    @Test
    public void testTicket18137() throws IOException, IllegalDataException, FileNotFoundException {
        try (InputStream fis = TestUtils.getRegressionDataStream(18137, "18137_npe.osm")) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, null, null));

            bib.startTest(null);
            bib.setBeforeUpload(true);
            bib.visit(ds.allPrimitives());
            bib.endTest();
            assertThat(bib.getErrors(), hasSize(2));
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19568">Bug #19568</a>.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if the OSM data cannot be parsed
     * @throws FileNotFoundException if the data file cannot be found
     */
    @Test
    public void testTicket19568() throws IOException, IllegalDataException, FileNotFoundException {
        try (InputStream fis = TestUtils.getRegressionDataStream(19568, "data.osm")) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, null, null));

            bib.startTest(null);
            bib.setBeforeUpload(false);
            bib.visit(ds.allPrimitives());
            bib.endTest();
            assertThat(bib.getErrors(), isEmpty());
        }
    }
}
