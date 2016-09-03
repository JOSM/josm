// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.io.OsmReader;
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
    public JOSMTestRules test = new JOSMTestRules().commands();

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
     */
    @Test
    public void testTicket10469() {
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
        try (InputStream is = new FileInputStream("data_nodist/multipolygon.osm")) {
            for (Relation r : OsmReader.parseDataSet(is, null).getRelations()) {
                if (r.isMultipolygon()) {
                    String name = DefaultNameFormatter.getInstance().format(r);
                    String codes = r.get("josm_error_codes");
                    if (codes != null) {
                        List<TestError> errors = new ArrayList<>();
                        for (org.openstreetmap.josm.data.validation.Test test : Arrays.asList(MULTIPOLYGON_TEST, RELATION_TEST)) {
                            test.initialize();
                            test.startTest(null);
                            test.visit(r);
                            test.endTest();
                            errors.addAll(test.getErrors());
                        }
                        Set<Integer> expectedCodes = new TreeSet<>();
                        for (String code : codes.split(",")) {
                            expectedCodes.add(Integer.parseInt(code));
                        }
                        Set<Integer> actualCodes = new TreeSet<>();
                        for (TestError error : errors) {
                            Integer code = error.getCode();
                            assertTrue(name + " does not expect JOSM error code " + code + ": " + error.getDescription(),
                                    expectedCodes.contains(code));
                            actualCodes.add(code);
                        }
                        assertEquals(name + " " + expectedCodes + " => " + actualCodes,
                                expectedCodes.size(), actualCodes.size());
                    } else if (r.hasKey("name") && (r.getName().startsWith("06") || r.getName().startsWith("07"))) {
                        fail(name + " lacks josm_error_codes tag");
                    }
                }
            }
        }
    }
}
