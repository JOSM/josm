// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RotationAngleTest {

    @Test
    public void testParseCardinal() throws Exception {
        assertThat(RotationAngle.buildStaticRotation("south").getRotationAngle(null), is(Math.PI));
        assertThat(RotationAngle.buildStaticRotation("s").getRotationAngle(null), is(Math.PI));
        assertThat(RotationAngle.buildStaticRotation("northwest").getRotationAngle(null), is(Math.toRadians(315)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseFail() throws Exception {
        RotationAngle.buildStaticRotation("bad");
    }

    @Test(expected = NullPointerException.class)
    public void testParseNull() throws Exception {
        RotationAngle.buildStaticRotation(null);
    }
}
