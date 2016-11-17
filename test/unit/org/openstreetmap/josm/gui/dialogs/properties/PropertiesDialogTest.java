// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PropertiesDialog} class.
 */
public class PropertiesDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static String createSearchSetting(DataSet ds, boolean sameType) {
        List<OsmPrimitive> sel = new ArrayList<>(ds.allPrimitives());
        Collections.sort(sel, OsmPrimitiveComparator.comparingUniqueId());
        return PropertiesDialog.createSearchSetting("foo", sel, sameType).text;
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12504">#12504</a>.
     */
    @Test
    public void testTicket12504() {
        DataSet ds = new DataSet();
        // 160 objects with foo=bar, 400 objects without foo
        for (int i = 0; i < 160+400; i++) {
            Node n = new Node(LatLon.ZERO);
            if (i < 160) {
                n.put("foo", "bar");
            }
            ds.addPrimitive(n);
        }
        assertEquals("(\"foo\"=\"bar\")", createSearchSetting(ds, false));

        Node n = new Node(LatLon.ZERO);
        n.put("foo", "baz");
        ds.addPrimitive(n);

        assertEquals("(\"foo\"=\"baz\") OR (\"foo\"=\"bar\")", createSearchSetting(ds, false));

        ds.removePrimitive(n);

        Way w = new Way();
        w.put("foo", "bar");
        ds.addPrimitive(w);

        assertEquals("(type:way \"foo\"=\"bar\") OR (type:node \"foo\"=\"bar\")", createSearchSetting(ds, true));
    }
}
