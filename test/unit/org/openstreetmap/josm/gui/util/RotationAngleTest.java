package org.openstreetmap.josm.gui.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RotationAngleTest {

    @Test
    public void testParseRad() throws Exception {
        assertThat(RotationAngle.buildStaticRotation("0.54rad").getRotationAngle(null), is(0.54));
        assertThat(RotationAngle.buildStaticRotation("1.").getRotationAngle(null), is(1.));
    }

    @Test
    public void testParseDeg() throws Exception {
        assertThat(RotationAngle.buildStaticRotation("180°").getRotationAngle(null), is(Math.PI));
        assertThat(RotationAngle.buildStaticRotation("90deg").getRotationAngle(null), is(Math.PI / 2));
    }

    @Test
    public void testParseCardinal() throws Exception {
        assertThat(RotationAngle.buildStaticRotation("south").getRotationAngle(null), is(Math.PI));
        assertThat(RotationAngle.buildStaticRotation("s").getRotationAngle(null), is(Math.PI));
        assertThat(RotationAngle.buildStaticRotation("northwest").getRotationAngle(null), is(Math.toRadians(315)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseFail() throws Exception {
        RotationAngle.buildStaticRotation("0.54bad");
    }

    @Test(expected = NullPointerException.class)
    public void testParseNull() throws Exception {
        RotationAngle.buildStaticRotation(null);
    }
}
