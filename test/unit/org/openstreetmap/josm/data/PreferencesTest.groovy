package org.openstreetmap.josm.data

import org.openstreetmap.josm.Main

import java.awt.Color

class PreferencesTest extends GroovyTestCase {
    @Override
    void setUp() {
        Main.initApplicationPreferences()
    }

    void testColorName() {
        assert Main.pref.getColorName("color.layer {5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx") == "color.layer {5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx"
    }

    void testColorAlpha() {
        assert Main.pref.getColor("foo", new Color(0x12345678, true)).alpha == 0x12
        assert Main.pref.putColor("bar", new Color(0x12345678, true))
        assert Main.pref.getColor("bar", null).alpha == 0x12
    }

    void testColorNameAlpha() {
        assert Main.pref.getColor("foo", "bar", new Color(0x12345678, true)).alpha == 0x12
        assert Main.pref.getDefaultColor("foo") == new Color(0x34, 0x56, 0x78, 0x12)
        assert Main.pref.getDefaultColor("foo").alpha == 0x12
    }
}
