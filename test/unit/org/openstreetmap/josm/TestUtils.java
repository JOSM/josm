// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.TextTagParser;

public class TestUtils {

    /**
     * Returns the path to test data root directory.
     */
    public static String getTestDataRoot() {
        String testDataRoot = System.getProperty("josm.test.data");
        if (testDataRoot == null || testDataRoot.isEmpty()) {
            testDataRoot = "test/data";
            System.out.println("System property josm.test.data is not set, using '" + testDataRoot + "'");
        }
        return testDataRoot.endsWith("/") ? testDataRoot : testDataRoot + "/";
    }

    public static OsmPrimitive createPrimitive(String assertion) {
        if (Main.pref == null) {
            Main.initApplicationPreferences();
        }
        final String[] x = assertion.split("\\s+", 2);
        final OsmPrimitive p = "n".equals(x[0]) || "node".equals(x[0])
                ? new Node()
                : "w".equals(x[0]) || "way".equals(x[0])
                ? new Way()
                : "r".equals(x[0]) || "relation".equals(x[0])
                ? new Relation()
                : null;
        if (p == null) {
            throw new IllegalArgumentException("Expecting n/node/w/way/r/relation, but got " + x[0]);
        }
        for (final Map.Entry<String, String> i : TextTagParser.readTagsFromText(x[1]).entrySet()) {
            p.put(i.getKey(), i.getValue());
        }
        return p;
    }

    @Test
    public void testCreatePrimitive() throws Exception {
        final OsmPrimitive p = createPrimitive("way name=Foo railway=rail");
        assertTrue(p instanceof Way);
        assertThat(p.keySet().size(), is(2));
        assertThat(p.get("name"), is("Foo"));
        assertThat(p.get("railway"), is("rail"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePrimitiveFail() throws Exception {
        TestUtils.createPrimitive("noway name=Foo");
    }

    /**
     * Checks that the given Comparator respects its contract on the given table.
     * @param comparator The comparator to test
     * @param array The array sorted for test purpose
     */
    public static <T> void checkComparableContract(Comparator<T> comparator, T[] array) {
        System.out.println("Validating Comparable contract on array of "+array.length+" elements");
        // Check each compare possibility
        for (int i=0; i<array.length; i++) {
            T r1 = array[i];
            for (int j=i; j<array.length; j++) {
                T r2 = array[j];
                int a = comparator.compare(r1, r2);
                int b = comparator.compare(r2, r1);
                if (i==j || a==b) {
                    if (a != 0 || b != 0) {
                        fail(getFailMessage(r1, r2, a, b));
                    }
                } else {
                    if (a != -b) {
                        fail(getFailMessage(r1, r2, a, b));
                    }
                }
                for (int k=j; k<array.length; k++) {
                    T r3 = array[k];
                    int c = comparator.compare(r1, r3);
                    int d = comparator.compare(r2, r3);
                    if (a > 0 && d > 0) {
                        if (c <= 0) {
                           fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    } else if (a == 0 && d == 0) {
                        if (c != 0) {
                            fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    } else if (a < 0 && d < 0) {
                        if (c >= 0) {
                            fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    }
                }
            }
        }
        // Sort relation array
        Arrays.sort(array, comparator);
    }

    private static <T> String getFailMessage(T o1, T o2, int a, int b) {
        return new StringBuilder("Compared\no1: ").append(o1).append("\no2: ")
        .append(o2).append("\ngave: ").append(a).append("/").append(b)
        .toString();
    }

    private static <T> String getFailMessage(T o1, T o2, T o3, int a, int b, int c, int d) {
        return new StringBuilder(getFailMessage(o1, o2, a, b))
        .append("\nCompared\no1: ").append(o1).append("\no3: ").append(o3).append("\ngave: ").append(c)
        .append("\nCompared\no2: ").append(o2).append("\no3: ").append(o3).append("\ngave: ").append(d)
        .toString();
    }
}
