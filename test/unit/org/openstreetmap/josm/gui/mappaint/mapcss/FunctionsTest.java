// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openstreetmap.josm.data.osm.OsmPrimitiveType.NODE;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.MapPaintStyles;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.Territories;

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

    private static Method[] FUNCTIONS;

    static Stream<Method> getFunctions() {
        if (FUNCTIONS == null) {
            FUNCTIONS = Stream.of(Functions.class.getDeclaredMethods())
                    .filter(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()))
                    .toArray(Method[]::new);
        }
        return Stream.of(FUNCTIONS);
    }

    @AfterAll
    static void tearDown() {
        FUNCTIONS = null;
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
    @MapPaintStyles
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

    @Test
    void testConvertPrimitivesToString() {
        assertEquals(Collections.singletonList("n1"), Functions.convert_primitives_to_string(
                Collections.singleton(new SimplePrimitiveId(1, NODE))));
        assertEquals(Arrays.asList("n1", "n9223372036854775807"), Functions.convert_primitives_to_string(
                Arrays.asList(new SimplePrimitiveId(1, NODE), new SimplePrimitiveId(Long.MAX_VALUE, NODE))));
    }

    @Test
    void testParentOsmPrimitives() {
        final Environment env = new EnvBuilder(NODE).build();
        final Relation relation1 = TestUtils.newRelation("", new RelationMember("", (Node) env.osm));
        final Relation relation2 = TestUtils.newRelation("type=something", new RelationMember("", (Node) env.osm));
        final Relation relation3 = TestUtils.newRelation("type=somethingelse", new RelationMember("", (Node) env.osm));

        TestUtils.addFakeDataSet((Node) env.osm);
        for (Relation relation : Arrays.asList(relation1, relation2, relation3)) {
            ((Node) env.osm).getDataSet().addPrimitive(relation);
        }

        final List<IPrimitive> allReferrers = Functions.parent_osm_primitives(env);
        assertAll(() -> assertEquals(3, allReferrers.size()),
                () -> assertTrue(allReferrers.contains(relation1)),
                () -> assertTrue(allReferrers.contains(relation2)),
                () -> assertTrue(allReferrers.contains(relation3)));

        final List<IPrimitive> typeReferrers = Functions.parent_osm_primitives(env, "type");
        assertAll(() -> assertEquals(2, typeReferrers.size()),
                () -> assertFalse(typeReferrers.contains(relation1)),
                () -> assertTrue(typeReferrers.contains(relation2)),
                () -> assertTrue(typeReferrers.contains(relation3)));

        final List<IPrimitive> typeSomethingReferrers = Functions.parent_osm_primitives(env, "type", "something");
        assertAll(() -> assertEquals(1, typeSomethingReferrers.size()),
                () -> assertSame(relation2, typeSomethingReferrers.get(0)));

        assertTrue(Functions.parent_osm_primitives(env, "type2").isEmpty());
    }

    /**
     * Non-regression test for #23238: NPE when env.osm is null
     */
    @ParameterizedTest
    @MethodSource("getFunctions")
    @Territories // needed for inside, outside, is_right_hand_traffic
    void testNonRegression23238(Method function) {
        if (function.getParameterCount() >= 1 && function.getParameterTypes()[0].isAssignableFrom(Environment.class)
         && !function.getParameterTypes()[0].equals(Object.class)) {
            Environment nullOsmEnvironment = new Environment();
            nullOsmEnvironment.mc = new MultiCascade();
            Object[] args = new Object[function.getParameterCount()];
            args[0] = nullOsmEnvironment;
            for (int i = 1; i < function.getParameterCount(); i++) {
                final Class<?> type = function.getParameterTypes()[i];
                if (String.class.isAssignableFrom(type)) {
                    args[i] = "";
                } else if (String[].class.isAssignableFrom(type)) {
                    args[i] = new String[] {"{0}", ""}; // join and tr require at least 2 arguments
                } else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
                    args[i] = 0d;
                } else if (Object.class.isAssignableFrom(type)) {
                    args[i] = new Object[0];
                } else {
                    fail(type.getCanonicalName());
                }
            }
            assertDoesNotThrow(() -> function.invoke(null, args));
        }
    }
}
