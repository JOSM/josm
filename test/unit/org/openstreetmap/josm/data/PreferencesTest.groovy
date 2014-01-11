package org.openstreetmap.josm.data

import org.openstreetmap.josm.Main

class PreferencesTest extends GroovyTestCase {
    @Override
    void setUp() {
        Main.initApplicationPreferences()
    }

    void testColorName() {
        Main.pref.getColorName("color.layer {5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx") == "{5DE308C0-916F-4B5A-B3DB-D45E17F30172}.gpx"
    }
}
