// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link InspectPrimitiveDialog} class.
 */
public class InspectPrimitiveDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().platform().projection().mapStyles();

    /**
     * Unit test of {@link InspectPrimitiveDialog#genericMonospacePanel}.
     */
    @Test
    public void testGenericMonospacePanel() {
        assertNotNull(InspectPrimitiveDialog.genericMonospacePanel(new JPanel(), ""));
    }

    /**
     * Unit test of {@link InspectPrimitiveDialog#buildDataText}.
     */
    @Test
    public void testBuildDataText() {
        DataSet ds = new DataSet();
        assertEquals("", InspectPrimitiveDialog.buildDataText(ds, new ArrayList<>(ds.allPrimitives())));
        Node n = new Node(LatLon.ZERO);
        n.setOsmId(1, 1);
        ds.addPrimitive(n);
        assertEquals(
                "Node: 1\n" +
                "  Data Set: "+Integer.toHexString(ds.hashCode())+"\n" +
                "  Edited at: <new object>\n" +
                "  Edited by: <new object>\n" +
                "  Version: 1\n" +
                "  In changeset: 0\n" +
                "  Coordinates: 0.0, 0.0\n" +
                "  Coordinates (projected): 0.0, -7.081154551613622E-10\n" +
                "\n", InspectPrimitiveDialog.buildDataText(ds, new ArrayList<>(ds.allPrimitives())));
    }

    /**
     * Unit test of {@link InspectPrimitiveDialog#buildListOfEditorsText}.
     */
    @Test
    public void testBuildListOfEditorsText() {
        DataSet ds = new DataSet();
        assertEquals("0 users last edited the selection:\n\n", InspectPrimitiveDialog.buildListOfEditorsText(ds.allPrimitives()));
        ds.addPrimitive(new Node(LatLon.ZERO));
        assertEquals("0 users last edited the selection:\n\n", InspectPrimitiveDialog.buildListOfEditorsText(ds.allPrimitives()));
        Node n = new Node(LatLon.ZERO);
        n.setUser(User.getAnonymous());
        ds.addPrimitive(n);
        n = new Node(LatLon.ZERO);
        n.setUser(User.getAnonymous());
        ds.addPrimitive(n);
        assertEquals(
                "1 user last edited the selection:\n" +
                "\n" +
                "     2  <anonymous>\n",
                InspectPrimitiveDialog.buildListOfEditorsText(ds.allPrimitives()));
    }

    /**
     * Unit test of {@link InspectPrimitiveDialog#buildMapPaintText}.
     */
    @Test
    public void testBuildMapPaintText() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);

        // CHECKSTYLE.OFF: LineLength
        String baseText =
                "Styles Cache for \"node ‎(0.0, 0.0)\":\n" +
                "\n" +
                "> applying mapcss style \"JOSM default (MapCSS)\"\n" +
                "\n" +
                "Range:|s119.4328566955879-Infinity\n" +
                " default: \n" +
                "Cascade{ font-size:8.0; major-z-index:4.95; symbol-fill-color:#ff0000; symbol-shape:Keyword{square}; symbol-size:6.0; symbol-stroke-color:#ff0000; }\n" +
                "\n" +
                "> skipping \"Potlatch 2\" (not active)\n" +
                "\n" +
                "List of generated Styles:\n" +
                " * NodeElement{z_idx=[4.95/0.0/0.0]  symbol=[symbolShape=SQUARE size=6 stroke=java.awt.BasicStroke strokeColor=java.awt.Color[r=255,g=0,b=0] fillColor=java.awt.Color[r=255,g=0,b=0]]}\n" +
                "\n" +
                "\n";
        // CHECKSTYLE.ON: LineLength

        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertEquals("", InspectPrimitiveDialog.buildMapPaintText());
            Node n = new Node(LatLon.ZERO);
            n.setUser(User.getAnonymous());
            ds.addPrimitive(n);
            ds.addSelected(n);
            String text = InspectPrimitiveDialog.buildMapPaintText().replaceAll("@(\\p{XDigit})+", "");
            assertEquals(baseText, text);
            n = new Node(LatLon.ZERO);
            n.setUser(User.getAnonymous());
            ds.addPrimitive(n);
            ds.addSelected(n);
            assertEquals(baseText + baseText + "The 2 selected objects have identical style caches.",
                    InspectPrimitiveDialog.buildMapPaintText().replaceAll("@(\\p{XDigit})+", ""));
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
