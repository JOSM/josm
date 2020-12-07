// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.AlignInCircleAction.InvalidSelection;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.opentest4j.AssertionFailedError;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link AlignInLineAction}.
 */
final class AlignInCircleActionTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();


    /**
     * Test case: way with several nodes selected
     * @throws Exception if an error occurs
     */
    @Test
    void testWaySelected() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleBefore.osm")), null);
        DataSet ds2 = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleAfter1.osm")), null);

        Way roundabout = null;
        for (Way w : ds.getWays()) {
            if ("roundabout".equals(w.get("junction"))) {
                roundabout = w;
                break;
            }
        }
        assertNotNull(roundabout);
        if (roundabout != null) {
            ds.setSelected(roundabout);
            Command c = AlignInCircleAction.buildCommand(ds);
            c.executeCommand();
            Way expected = (Way) ds2.getPrimitiveById(roundabout);
            assertNotNull(expected);
            assertEquals(expected, roundabout);
            assertEquals(expected.getNodesCount(), roundabout.getNodesCount());
            for (Node n1 : roundabout.getNodes()) {
                Node n2 = (Node) ds2.getPrimitiveById(n1);
                assertEquals(n1.lat(), n2.lat(), 1e-5);
                assertEquals(n1.lon(), n2.lon(), 1e-5);
            }
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20041">Bug #20041</a>.
     * Don't create move commands when no node is visibly moved.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20041() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleAfter1.osm")), null);

        Way roundabout = null;
        for (Way w : ds.getWays()) {
            if ("roundabout".equals(w.get("junction"))) {
                roundabout = w;
                break;
            }
        }
        assertNotNull(roundabout);
        if (roundabout != null) {
            ds.setSelected(roundabout);
            assertNull(AlignInCircleAction.buildCommand(ds));
        }
    }

    /**
     * Test case: way with several nodes selected
     * @throws Exception if an error occurs
     */
    @Test
    void testNodesSelected() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleBefore.osm")), null);
        DataSet ds2 = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleAfter2.osm")), null);

        Way circularWay = null;
        for (Way w : ds.getWays()) {
            if ("roundabout".equals(w.get("junction"))) {
                circularWay = w;
                break;
            }
        }
        assertNotNull(circularWay);
        if (circularWay != null) {
            ds.setSelected(circularWay.getNodes());
            Command c = AlignInCircleAction.buildCommand(ds);
            assertNotNull(c);
            c.executeCommand();
            Way expected = (Way) ds2.getPrimitiveById(circularWay);
            assertNotNull(expected);
            assertEquals(expected, circularWay);
            assertEquals(expected.getNodesCount(), circularWay.getNodesCount());
            for (Node n1 : circularWay.getNodes()) {
                Node n2 = (Node) ds2.getPrimitiveById(n1);
                assertEquals(n1.lat(), n2.lat(), 1e-5);
                assertEquals(n1.lon(), n2.lon(), 1e-5);
            }
        }
    }

    /**
     * Test case: original roundabout was split, two ways selected, they build a closed ring
     * @throws Exception if an error occurs
     */
    @Test
    void testOpenWaysSelected() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleTwoWaysBefore.osm")), null);
        DataSet ds2 = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleTwoWaysAfter.osm")), null);

        Set<Way> junctions = ds.getWays().stream().filter(w -> "roundabout".equals(w.get("junction"))).collect(Collectors.toSet());
        assertEquals(2, junctions.size());
        ds.setSelected(junctions);
        Command c = AlignInCircleAction.buildCommand(ds);
        assertNotNull(c);
        c.executeCommand();
        for (Way way : junctions) {
            for (Node n1 : way.getNodes()) {
                Node n2 = (Node) ds2.getPrimitiveById(n1);
                assertEquals(n1.lat(), n2.lat(), 1e-5);
                assertEquals(n1.lon(), n2.lon(), 1e-5);
            }
        }
    }

    /**
     * Various cases of selections in file
     * @throws Exception if an error occurs
     */
    @Test
    void testSelectionEvaluation() throws Exception {
        DataSet ds = OsmReader.parseDataSet(
                Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "alignCircleCases.osm")), null);

        for (int i = 0; i < 80; i++) {
            final String selVal = Integer.toString(i);
            Set<OsmPrimitive> sel = ds.allPrimitives().stream().filter(p -> p.hasTag("sel", selVal))
                    .collect(Collectors.toSet());
            if (sel.isEmpty())
                continue;
            ds.setSelected(sel);
            boolean selValid = sel.stream().noneMatch(p -> p.hasKey("invalid-selection"));
            try {
                AlignInCircleAction.buildCommand(ds);
                assertTrue(selValid, "sel=" + selVal + " is not valid?");
            } catch (InvalidSelection e) {
                assertFalse(selValid, "sel=" + selVal + " is not invalid?");
            } catch (Exception e) {
                throw new AssertionFailedError("test failed: sel=" + selVal,e);
            }
        }
    }
}
