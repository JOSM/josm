// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.io.FileInputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests for class {@link OsmDataLayer}.
 */
public class OrthogonalizeActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

    @Test(expected = OrthogonalizeAction.InvalidUserInputException.class)
    public void testNoSelection() throws Exception {
        performTest("nothing selected");
    }

    @Test
    public void testClosedWay() throws Exception {
        performTest("name=ClosedWay");
    }

    @Test
    public void testTwoWaysFormingClosedWay() throws Exception {
        performTest("name=TwoWaysFormingClosedWay");
    }

    @Test
    public void testTwoRingsAtOnce() throws Exception {
        performTest("name=ClosedWay OR name=TwoWaysFormingClosedWay");
    }

    @Test
    public void testClosedWayWithReferenceNodes() throws Exception {
        performTest("name=ClosedWayWithReferenceNodes");
    }

    void performTest(String search) throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "orthogonalize.osm")) {
            final DataSet ds = OsmReader.parseDataSet(in, null);
            ds.setSelected(Utils.filter(ds.allPrimitives(), SearchCompiler.compile(search)));
            OrthogonalizeAction.orthogonalize(ds.getSelected());
        }
    }
}
