// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests

import org.openstreetmap.josm.Main
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference

class ConditionalKeysTest extends GroovyTestCase {

    def test = new ConditionalKeys()

    @Override
    void setUp() {
        Main.initApplicationPreferences();
        TaggingPresetPreference.readFromPreferences()
        test.initialize()
    }

    void testKeyValid() {
        assert test.isKeyValid("maxspeed:conditional")
        assert test.isKeyValid("motor_vehicle:conditional")
        assert test.isKeyValid("bicycle:conditional")
        assert test.isKeyValid("overtaking:hgv:conditional")
        assert test.isKeyValid("maxspeed:hgv:backward:conditional")
        assert test.isKeyValid("oneway:backward:conditional")
        assert test.isKeyValid("fee:conditional")
        assert !test.isKeyValid("maxspeed:hgv:conditional:backward")
    }

    void testValueValid() {
        assert test.isValueValid("maxspeed:conditional", "120 @ (06:00-19:00)")
        assert !test.isValueValid("maxspeed:conditional", " @ (06:00-19:00)")
        assert !test.isValueValid("maxspeed:conditional", "120 (06:00-19:00)")
        assert !test.isValueValid("maxspeed:conditional", "120 @ ()")
        assert !test.isValueValid("maxspeed:conditional", "120 @ ")
        assert !test.isValueValid("maxspeed:conditional", "120 @ (06:00/19:00)")
        assert test.isValueValid("maxspeed:conditional", "120 @ (06:00-20:00); 100 @ (22:00-06:00)")
        assert test.isValueValid("motor_vehicle:conditional", "delivery @ (Mo-Fr 06:00-11:00,17:00-19:00;Sa 03:30-19:00)")
        assert test.isValueValid("motor_vehicle:conditional", "no @ (10:00-18:00 AND length>5)")
        assert !test.isValueValid("motor_vehicle:conditional", "foo @ (10:00-18:00 AND length>5)")
        assert !test.isValueValid("motor_vehicle:conditional", "no @ (10:00until18:00 AND length>5)")
        assert test.isValueValid("maxspeed:hgv:conditional", "60 @ (weight>7.5)")

    }
}
