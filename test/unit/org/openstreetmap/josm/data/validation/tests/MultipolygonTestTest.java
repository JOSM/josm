// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of Multipolygon validation test.
 */
public class MultipolygonTestTest {

    private static final MultipolygonTest MULTIPOLYGON_TEST = new MultipolygonTest();
    private static final RelationChecker RELATION_TEST = new RelationChecker();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().mapStyles().presets().main();

    private static Way createUnclosedWay(String tags) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(new LatLon(0, 1)));
        nodes.add(new Node(new LatLon(0, 2)));

        Way w = (Way) OsmUtils.createPrimitive("way "+tags);
        w.setNodes(nodes);
        return w;
    }

    /**
     * Non-regression test for bug #10469.
     * @throws Exception if an exception occurs
     */
    @Test
    public void testTicket10469() throws Exception {
        MULTIPOLYGON_TEST.initialize();
        MULTIPOLYGON_TEST.startTest(null);

        // Erroneous tag
        Way w = createUnclosedWay("amenity=parking");
        MULTIPOLYGON_TEST.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, MULTIPOLYGON_TEST.getErrors().size());

        // Erroneous tag, but managed by another test
        w = createUnclosedWay("building=yes");
        MULTIPOLYGON_TEST.visit(w);
        assertTrue(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, MULTIPOLYGON_TEST.getErrors().size());

        // Correct tag, without area style since #10601 (r7603)
        w = createUnclosedWay("aeroway=taxiway");
        MULTIPOLYGON_TEST.visit(w);
        assertFalse(ElemStyles.hasAreaElemStyle(w, false));
        assertEquals(1, MULTIPOLYGON_TEST.getErrors().size());

        MULTIPOLYGON_TEST.endTest();
    }

    /**
     * Test all error cases manually created in multipolygon.osm.
     * @throws Exception in case of error
     */
    @Test
    public void testMultipolygonFile() throws Exception {
        ValidatorTestUtils.testSampleFile("data_nodist/multipolygon.osm",
                ds -> ds.getRelations().stream().filter(Relation::isMultipolygon).collect(Collectors.toList()),
                name -> name.startsWith("06") || name.startsWith("07") || name.startsWith("08"), MULTIPOLYGON_TEST, RELATION_TEST);
    }
}
