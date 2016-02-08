// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Unit tests of {@link PropertiesDialog} class.
 */
public class PropertiesDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
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
        assertEquals("(\"foo\"=\"bar\")",
                PropertiesDialog.createSearchSetting("foo", ds.allPrimitives(), false).text);

        Node n = new Node(LatLon.ZERO);
        n.put("foo", "baz");
        ds.addPrimitive(n);

        assertEquals("(\"foo\"=\"bar\") OR (\"foo\"=\"baz\")",
                PropertiesDialog.createSearchSetting("foo", ds.allPrimitives(), false).text);

        ds.removePrimitive(n);

        Way w = new Way();
        w.put("foo", "bar");
        ds.addPrimitive(w);

        assertEquals("(type:node \"foo\"=\"bar\") OR (type:way \"foo\"=\"bar\")",
                PropertiesDialog.createSearchSetting("foo", ds.allPrimitives(), true).text);
    }
}
