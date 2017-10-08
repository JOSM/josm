// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Preferences}.
 */
public class PreferencesTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().preferences().fakeAPI();

    /**
     * Test color name.
     */
    @Test
    public void testColorName() {
        assertEquals("Layer: {5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx",
                Main.pref.getColorName("layer {5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx"));
    }

    /**
     * Test color alpha.
     */
    @Test
    public void testColorAlpha() {
        assertEquals(0x12, new ColorProperty("foo", new Color(0x12345678, true)).get().getAlpha());
        assertTrue(Main.pref.putColor("bar", new Color(0x12345678, true)));
        assertEquals(0x12, new ColorProperty("bar", Color.RED).get().getAlpha());
    }

    /**
     * Test color name and alpha.
     */
    @Test
    public void testColorNameAlpha() {
        assertEquals(0x12, new ColorProperty("foo", new Color(0x12345678, true)).get().getAlpha());
        assertEquals(new Color(0x34, 0x56, 0x78, 0x12), Main.pref.getDefaultColor("foo"));
        assertEquals(0x12, Main.pref.getDefaultColor("foo").getAlpha());
    }

    /**
     * Test {@link Preferences#toXML}.
     */
    @Test
    public void testToXml() {
        assertEquals(String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
            "<preferences xmlns='http://josm.openstreetmap.de/preferences-1.0' version='%d'>%n" +
            "  <tag key='osm-server.url' value='http://fake.xxx/api'/>%n" +
            "</preferences>%n", Version.getInstance().getVersion()),
                Main.pref.toXML(true));
    }
}
