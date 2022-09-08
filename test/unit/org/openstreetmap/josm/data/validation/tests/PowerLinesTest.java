// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test class for {@link PowerLines}
 * @since 18553
 */
@BasicPreferences
class PowerLinesTest {
    private PowerLines powerLines;
    private DataSet ds;

    @RegisterExtension
    static JOSMTestRules josmTestRules = new JOSMTestRules().projection();

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
}
