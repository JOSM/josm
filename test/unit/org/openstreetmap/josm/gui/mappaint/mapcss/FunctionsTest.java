// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.data.osm.OsmPrimitiveType.NODE;

import java.util.Collections;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link Functions}.
 */
@BasicPreferences
@Projection
class FunctionsTest {
    private static class EnvBuilder {
        private final OsmPrimitive osm;

        EnvBuilder(OsmPrimitiveType type) {
            switch (type) {
                case NODE : osm = TestUtils.newNode(""); break;
                case WAY : osm = TestUtils.newWay(""); break;
                case RELATION : osm = TestUtils.newRelation(""); break;
                default: throw new IllegalArgumentException();
            }
        }

        EnvBuilder setUser(User user) {
            osm.setUser(user);
            return this;
        }

        Environment build() {
            return new Environment(osm);
        }
    }

    /**
     * Unit test of {@link Functions#title}.
     */
    @Test
    void testTitle() {
        assertNull(Functions.title(null));
        assertEquals("", Functions.title(""));
        assertEquals("I Am Fine", Functions.title("i am FINE"));
    }

    /**
     * Unit test of {@link Functions#osm_user_name}.
     */
    @Test
    void testOsmUserName() {
        assertEquals("<anonymous>", Functions.osm_user_name(new EnvBuilder(NODE).setUser(User.getAnonymous()).build()));
    }

    /**
     * Unit test of {@link Functions#osm_user_id}.
     */
    @Test
    void testOsmUserId() {
        assertEquals(-1, Functions.osm_user_id(new EnvBuilder(NODE).setUser(User.getAnonymous()).build()));
    }

    /**
     * Unit test of {@link Functions#osm_version}.
     */
    @Test
    void testOsmVersion() {
        assertEquals(0, Functions.osm_version(new EnvBuilder(NODE).build()));
    }

    /**
     * Unit test of {@link Functions#osm_changeset_id}.
     */
    @Test
    void testOsmChangesetId() {
        assertEquals(0, Functions.osm_changeset_id(new EnvBuilder(NODE).build()));
    }

    /**
     * Unit test of {@link Functions#osm_timestamp}.
     */
    @Test
    void testOsmTimestamp() {
        assertEquals(0, Functions.osm_timestamp(new EnvBuilder(NODE).build()));
    }

    /**
     * Test for {@link Functions#parent_way_angle(Environment)}
     */
    @Test
    void testParentWayAngle() {
        assertNull(Functions.parent_way_angle(new EnvBuilder(NODE).build()));
        final Environment environment = new EnvBuilder(NODE).build();
        ((Node) environment.osm).setCoor(LatLon.ZERO);
        final Way parent = TestUtils.newWay("", new Node(new LatLon(-.1, 0)), (Node) environment.osm, new Node(new LatLon(.1, 0)));
        environment.parent = parent;
        Double actual = Functions.parent_way_angle(environment);
        assertNotNull(actual);
        assertEquals(Math.toRadians(0), actual, 1e-9);
        // Reverse node order
        Objects.requireNonNull(parent.firstNode()).setCoor(LatLon.NORTH_POLE);
        Objects.requireNonNull(parent.lastNode()).setCoor(LatLon.SOUTH_POLE);
        actual = Functions.parent_way_angle(environment);
        assertNotNull(actual);
        assertEquals(Math.toRadians(180), actual, 1e-9);
    }

    /**
     * Unit test of {@code Functions#to_xxx}
     */
    @Test
    void testParseFunctions() {
        assertTrue(Functions.to_boolean("true"));
        assertEquals(1, Functions.to_byte("1"));
        assertEquals(1, Functions.to_short("1"));
        assertEquals(1, Functions.to_int("1"));
        assertEquals(1L, Functions.to_long("1"));
        assertEquals(1f, Functions.to_float("1"), 1e-10);
        assertEquals(1d, Functions.to_double("1"), 1e-10);
    }

    /**
     * Unit test of {@link Functions#JOSM_pref}
     */
    @Test
    void testPref() {
        String key = "Functions.JOSM_pref";
        Config.getPref().put(key, null);
        assertEquals("foobar", Functions.JOSM_pref(null, key, "foobar"));
        Config.getPref().put(key, "baz");
        GuiHelper.runInEDTAndWait(() -> {
            // await org.openstreetmap.josm.gui.mappaint.ElemStyles.clearCached
        });
        assertEquals("baz", Functions.JOSM_pref(null, key, "foobar"));
        Config.getPref().put(key, null);
        GuiHelper.runInEDTAndWait(() -> {
            // await org.openstreetmap.josm.gui.mappaint.ElemStyles.clearCached
        });
        assertEquals("foobar", Functions.JOSM_pref(null, key, "foobar"));
        Config.getPref().put(key, null);
    }

    /**
     * Unit test of {@link Functions#JOSM_pref}, color handling
     */
    @Test
    void testPrefColor() {
        String key = "Functions.JOSM_pref";
        String colorKey = NamedColorProperty.NAMED_COLOR_PREFIX + NamedColorProperty.COLOR_CATEGORY_MAPPAINT + ".unknown." + key;
        Config.getPref().put(colorKey, null);
        assertEquals("#000000", Functions.JOSM_pref(null, key, "#000000"));
        Config.getPref().putList(colorKey, Collections.singletonList("#00FF00"));
        GuiHelper.runInEDTAndWait(() -> {
            // await org.openstreetmap.josm.gui.mappaint.ElemStyles.clearCached
        });
        assertEquals("#00FF00", Functions.JOSM_pref(null, key, "#000000"));
        Config.getPref().put(colorKey, null);
        GuiHelper.runInEDTAndWait(() -> {
            // await org.openstreetmap.josm.gui.mappaint.ElemStyles.clearCached
        });
        assertEquals("#000000", Functions.JOSM_pref(null, key, "#000000"));
        Config.getPref().put(colorKey, null);
    }
}
