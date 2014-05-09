// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.CustomMatchers.isEmpty;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;

public class UnconnectedWaysTest {

    UnconnectedWays bib;

    /**
     * Setup test.
     */
    @Before
    public void setUp() throws Exception {
        bib = new UnconnectedWays.UnconnectedHighways();
        JOSMFixture.createUnitTestFixture().init();
        bib.initialize();
        bib.startTest(null);
    }

    @Test
    public void testTicket6313() throws Exception {
        try (InputStream fis = new FileInputStream("data_nodist/UnconnectedWaysTest.osm")) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            bib.visit(ds.allPrimitives());
            bib.endTest();
            assertThat(bib.getErrors(), isEmpty());
        }
    }
}
