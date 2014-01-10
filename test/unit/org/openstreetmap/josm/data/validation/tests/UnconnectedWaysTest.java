// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.CustomMatchers.isEmpty;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;

public class UnconnectedWaysTest {

    UnconnectedWays bib;

    @Before
    public void setUp() throws Exception {
        bib = new UnconnectedWays.UnconnectedHighways();
        Main.initApplicationPreferences();
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        bib.initialize();
        bib.startTest(null);
    }

    @Test
    public void testTicket6313() throws Exception {
        final DataSet ds = OsmReader.parseDataSet(new FileInputStream("data_nodist/UnconnectedWaysTest.osm"), NullProgressMonitor.INSTANCE);
        bib.visit(ds.allPrimitives());
        bib.endTest();
        assertThat(bib.getErrors(), isEmpty());
    }
}
