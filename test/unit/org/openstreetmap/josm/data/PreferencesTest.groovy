// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data

import java.awt.Color

import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.preferences.ColorProperty

class PreferencesTest extends GroovyTestCase {
    @Override
    void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    void testColorName() {
        assert Main.pref.getColorName("color.layer {5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx") == "color.layer {5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx"
    }

    void testColorAlpha() {
        assert new ColorProperty("foo", new Color(0x12345678, true)).get().alpha == 0x12
        assert Main.pref.putColor("bar", new Color(0x12345678, true))
        assert new ColorProperty("bar", (String) null).get().alpha == 0x12
    }

    void testColorNameAlpha() {
        assert new ColorProperty("foo", new Color(0x12345678, true)).get().alpha == 0x12
        assert Main.pref.getDefaultColor("foo") == new Color(0x34, 0x56, 0x78, 0x12)
        assert Main.pref.getDefaultColor("foo").alpha == 0x12
    }

    void testToXml() {
        assert Main.pref.toXML(true) == String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
            "<preferences xmlns='http://josm.openstreetmap.de/preferences-1.0' version='%d'>%n" +
            "  <tag key='expert' value='true'/>%n" +
            "  <tag key='jdk.Arrays.useLegacyMergeSort' value='false'/>%n" +
            "  <tag key='language' value='en'/>%n" +
            "  <tag key='osm-server.url' value='http://api06.dev.openstreetmap.org/api'/>%n" +
            "  <tag key='osm-server.username' value='%s'/>%n" +
            "</preferences>%n", Version.getInstance().getVersion(), Main.pref.get("osm-server.username"))
    }
}
