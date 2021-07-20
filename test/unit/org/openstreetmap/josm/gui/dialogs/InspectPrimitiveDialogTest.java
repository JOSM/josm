// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openstreetmap.josm.TestUtils.assertEqualsNewline;

import java.util.ArrayList;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link InspectPrimitiveDialog} class.
 */
@BasicPreferences
class InspectPrimitiveDialogTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection().mapStyles();

    /**
     * Setup test
     */
    @BeforeEach
    public void setUp() {
        SystemOfMeasurement.PROP_SYSTEM_OF_MEASUREMENT.put("METRIC");

    }

    /**
     * Cleanup test
     */
    @AfterEach
    public void tearDown() {
        SystemOfMeasurement.PROP_SYSTEM_OF_MEASUREMENT.put(null);
    }

    /**
     * Unit test of {@link InspectPrimitiveDialog#genericMonospacePanel}.
     */
    @Test
    void testGenericMonospacePanel() {
        assertNotNull(InspectPrimitiveDialog.genericMonospacePanel(new JPanel(), ""));
    }

    /**
     * Unit test of {@link InspectPrimitiveDialog#buildDataText}.
     */
    @Test
    void testBuildDataText() {
        DataSet ds = new DataSet();
        assertEqualsNewline("", InspectPrimitiveDialog.buildDataText(ds, new ArrayList<>(ds.allPrimitives())));
        final Way way = new Way();
        way.addNode(new Node(new LatLon(47.2687921, 11.390525)));
        way.addNode(new Node(new LatLon(47.2689194, 11.3907301)));
        way.addNode(new Node(new LatLon(47.2684158, 11.3914047)));
        way.addNode(new Node(new LatLon(47.2682898, 11.3912034)));
        way.setOsmId(1, 1);
        int id = 2;
        for (Node node : way.getNodes()) {
            node.setOsmId(id, id);
            id++;
        }
        way.getNodes().forEach(ds::addPrimitive);
        ds.addPrimitive(way);
        way.addNode(way.firstNode()); // close way
        assertEqualsNewline(
            "Way: 1\n" +
                "  Data Set: "+Integer.toHexString(ds.hashCode())+"\n" +
                "  Edited at: <new object>\n" +
                "  Edited by: <new object>\n" +
                "  Version: 1\n" +
                "  In changeset: 0\n" +
                "  Bounding box: 47.2682898, 11.3914047, 47.2689194, 11.390525\n" +
                "  Bounding box (projected): 5985976.274977, 1268085.3706241, 5986079.5621105, 1267987.4428681\n" +
                "  Center of bounding box: 47.2686046, 11.3909648\n" +
                "  Centroid: 47.2686049, 11.3909649\n" +
                "  Length: 193.3 m\n" +
                "  5 Nodes: \n" +
                "    2\n" +
                "    3\n" +
                "    4\n" +
                "    5\n" +
                "    2\n" +
                "\n", InspectPrimitiveDialog.buildDataText(ds, new ArrayList<>(ds.getWays())));
    }

    /**
     * Unit test of {@link InspectPrimitiveDialog#buildListOfEditorsText}.
     */
    @Test
    void testBuildListOfEditorsText() {
        DataSet ds = new DataSet();
        assertEqualsNewline("0 users last edited the selection:\n\n", InspectPrimitiveDialog.buildListOfEditorsText(ds.allPrimitives()));
        ds.addPrimitive(new Node(LatLon.ZERO));
        assertEqualsNewline("0 users last edited the selection:\n\n", InspectPrimitiveDialog.buildListOfEditorsText(ds.allPrimitives()));
        Node n = new Node(LatLon.ZERO);
        n.setUser(User.getAnonymous());
        ds.addPrimitive(n);
        n = new Node(LatLon.ZERO);
        n.setUser(User.getAnonymous());
        ds.addPrimitive(n);
        assertEqualsNewline(
                "1 user last edited the selection:\n" +
                "\n" +
                "     2  <anonymous>\n",
                InspectPrimitiveDialog.buildListOfEditorsText(ds.allPrimitives()));
    }

    /**
     * Unit test of {@link InspectPrimitiveDialog#buildMapPaintText}.
     */
    @Test
    void testBuildMapPaintText() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);

        // CHECKSTYLE.OFF: LineLength
        String baseText =
                "Styles for \"node\":\n" +
                "==================\n" +
                "MapCSS style \"JOSM default (MapCSS)\"\n" +
                "------------------------------------\n" +
                "Display range: |z119.4329-Infinity\n" +
                "Layer default\n" +
                " * Cascade{ font-size:8.0; major-z-index:4.95; symbol-fill-color:#FF0000; symbol-shape:Keyword{square}; symbol-size:6.0; symbol-stroke-color:#FF0000; }\n" +
                "\n" +
                "List of generated Styles:\n" +
                "-------------------------\n" +
                " * NodeElement{z_idx=[4.95/0.0/0.0]  symbol=[symbolShape=SQUARE size=6 stroke=java.awt.BasicStroke strokeColor=java.awt.Color[r=255,g=0,b=0] fillColor=java.awt.Color[r=255,g=0,b=0]]}\n" +
                "\n" +
                "\n";
        // CHECKSTYLE.ON: LineLength

        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertEqualsNewline("", InspectPrimitiveDialog.buildMapPaintText());
            Node n = new Node(LatLon.ZERO);
            n.setUser(User.getAnonymous());
            ds.addPrimitive(n);
            ds.addSelected(n);
            String text = InspectPrimitiveDialog.buildMapPaintText().replaceAll("@(\\p{XDigit})+", "");
            assertEqualsNewline(baseText, text);
            n = new Node(LatLon.ZERO);
            n.setUser(User.getAnonymous());
            ds.addPrimitive(n);
            ds.addSelected(n);
            assertEqualsNewline(baseText + baseText + "The 2 selected objects have identical style caches.\n",
                    InspectPrimitiveDialog.buildMapPaintText().replaceAll("@(\\p{XDigit})+", ""));
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}

