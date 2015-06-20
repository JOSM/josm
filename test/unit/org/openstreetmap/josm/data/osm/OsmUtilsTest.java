// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

public class OsmUtilsTest {

    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testCreatePrimitive() throws Exception {
        final OsmPrimitive p = OsmUtils.createPrimitive("way name=Foo railway=rail");
        assertTrue(p instanceof Way);
        assertThat(p.keySet().size(), is(2));
        assertThat(p.get("name"), is("Foo"));
        assertThat(p.get("railway"), is("rail"));
    }

    @Test
    public void testArea() throws Exception {
        final OsmPrimitive p = OsmUtils.createPrimitive("area name=Foo railway=rail");
        assertThat(p.getType(), is(OsmPrimitiveType.WAY));
        assertTrue(p.getKeys().equals(OsmUtils.createPrimitive("way name=Foo railway=rail").getKeys()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePrimitiveFail() throws Exception {
        OsmUtils.createPrimitive("noway name=Foo");
    }

}
