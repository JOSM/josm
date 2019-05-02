// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.CustomMatchers.isEmpty;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
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
        bib.startTest(null);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/6313">Bug #6313</a>.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if the OSM data cannot be parsed
     * @throws FileNotFoundException if the data file cannot be found
     */
    @Test
    public void testTicket6313() throws IOException, IllegalDataException, FileNotFoundException {
        try (InputStream fis = Files.newInputStream(Paths.get("data_nodist/UnconnectedWaysTest.osm"))) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            bib.visit(ds.allPrimitives());
            bib.endTest();
            assertThat(bib.getErrors(), isEmpty());
        }
    }
}
