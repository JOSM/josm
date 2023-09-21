// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Test class for {@link PowerLines}
 * @since 18553
 */
@BasicPreferences
@Projection
class PowerLinesTest {
    private PowerLines powerLines;
    private DataSet ds;

    @BeforeEach
    public void setUp() throws Exception {
        ds = new DataSet();

        powerLines = new PowerLines();
        powerLines.initialize();
        powerLines.startTest(null);
    }

    @Test
    void testNoBreakInLine() {
        Way powerline = new Way();
        powerline.setKeys(new TagMap("power", "line"));
        ds.addPrimitive(powerline);

        for (int i = 0; i < 10; i++) {
            Node node = new Node(new LatLon(0, 0.001 * i));
            node.setKeys(new TagMap("power", "tower"));
            ds.addPrimitive(node);
            powerline.addNode(node);
        }
        powerLines.visit(powerline);
        powerLines.endTest();
        assertTrue(powerLines.getErrors().isEmpty());
    }

    @Test
    void testBreakInLine() {
        Way powerline = new Way();
        powerline.setKeys(new TagMap("power", "line"));
        ds.addPrimitive(powerline);

        for (int i = 0; i < 10; i++) {
            if (i != 4 && i != 5) {
                Node node = new Node(new LatLon(0, 0.001 * i));
                node.setKeys(new TagMap("power", "tower"));
                ds.addPrimitive(node);
                powerline.addNode(node);
            }
        }
        powerLines.visit(powerline);
        powerLines.endTest();
        assertFalse(powerLines.getErrors().isEmpty());
    }

    @Test
    void testConnectionAndRefInLine() {
        Way powerline = new Way();
        powerline.setKeys(new TagMap("power", "line"));
        ds.addPrimitive(powerline);

        int connectionCount = 0;

        for (int i = 0; i < 10; i++) {
            Node node = new Node(new LatLon(0, 0.001 * i));
            node.setKeys(new TagMap("power", "tower", "ref", Integer.toString(i)));
            if (i == 4 || i == 5) {
                node.setKeys(new TagMap("power", "connection"));
                connectionCount++;
            }
            if (i > 5) {
                node.setKeys(new TagMap("power", "tower", "ref", Integer.toString(i - connectionCount)));
            }
            ds.addPrimitive(node);
            powerline.addNode(node);
        }
        powerLines.visit(powerline);
        powerLines.endTest();
        assertTrue(powerLines.getErrors().isEmpty());
    }

    @Test
    void testRefDiscontinuityInLine() {
        Way powerline = new Way();
        powerline.setKeys(new TagMap("power", "minor_line"));
        ds.addPrimitive(powerline);

        for (int i = 0; i < 10; i++) {
            Node node = new Node(new LatLon(0, 0.001 * i));
            node.setKeys(new TagMap("power", "tower", "ref", Integer.toString(i)));
            if (i < 4) {
                // add discontinuity
                node.setKeys(new TagMap("power", "tower", "ref", Integer.toString(i + 1)));
            }
            ds.addPrimitive(node);
            powerline.addNode(node);
        }
        powerLines.visit(powerline);
        powerLines.endTest();
        assertFalse(powerLines.getErrors().isEmpty());
    }

    /**
     * Ensure that incomplete relations don't cause problems
     */
    @Test
    void testNonRegression22684() {
        final Relation powerLine = TestUtils.newRelation("natural=water water=river",
                new RelationMember("", TestUtils.newWay("", new Node(), new Node())));
        assertDoesNotThrow(() -> this.powerLines.visit(powerLine));
    }

    @Test
    void testPowerlineCrossingOcean() {
        final Way powerLine = TestUtils.newWay("power=line",
                new Node(new LatLon(52.6759313, 8.3845078)),
                new Node(new LatLon(52.6759857, 8.3898977)),
                new Node(new LatLon(52.6759857, 8.3949282)),
                new Node(new LatLon(52.6758768, 8.3978927)),
                new Node(new LatLon(52.6759313, 8.4322981)),
                new Node(new LatLon(52.6759857, 8.4419999)),
                new Node(new LatLon(52.6758768, 8.4488271)));
        final Way coastline = TestUtils.newWay("natural=coastline",
                new Node(new LatLon(52.6813231, 8.4253811)),
                new Node(new LatLon(52.6684687, 8.4254709)),
                new Node(new LatLon(52.6680328, 8.4033724)),
                new Node(new LatLon(52.6838282, 8.4070555)));
        for (Node node : powerLine.getNodes()) {
            node.put("power", "tower");
        }
        this.ds.addPrimitiveRecursive(powerLine);
        this.ds.addPrimitiveRecursive(coastline);

        this.powerLines.startTest(NullProgressMonitor.INSTANCE);
        this.powerLines.visit(new ArrayList<>(ds.getWays()));
        this.powerLines.endTest();
        assertTrue(this.powerLines.getErrors().isEmpty());
    }
}
