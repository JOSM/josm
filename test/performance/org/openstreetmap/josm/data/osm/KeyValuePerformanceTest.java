// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.PerformanceTestUtils.PerformanceTestTimer;
import org.openstreetmap.josm.data.osm.OsmDataGenerator.KeyValueDataGenerator;

/**
 * This test measures the performance of {@link OsmPrimitive#get(String)} and related.
 * @author Michael Zangl
 */
public class KeyValuePerformanceTest {
    private static final int PUT_RUNS = 10000;
    private static final int GET_RUNS = 100000;
    private static final int TEST_STRING_COUNT = 10000;
    private static final int STRING_INTERN_TESTS = 5000000;
    private static final double[] TAG_NODE_RATIOS = new double[] { .05, .3, 3, 20, 200 };
    private ArrayList<String> testStrings = new ArrayList<>();
    private Random random;

    /**
     * Prepare the test.
     */
    @BeforeClass
    public static void createJOSMFixture() {
        JOSMFixture.createPerformanceTestFixture().init(true);
    }

    /**
     * See if there is a big difference between Strings that are interned and those that are not.
     */
    @Test
    public void meassureStringEqualsIntern() {
        String str1Interned = "string1";
        String str1InternedB = "string1";
        String str1 = new String(str1Interned);
        String str1B = new String(str1Interned);
        String str2Interned = "string2";
        String str2 = new String(str2Interned);

        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            // warm up
            assertTrue(str1.equals(str1B));
        }

        PerformanceTestTimer timer = PerformanceTestUtils.startTimer("Assertion overhead.");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertTrue(true);
        }
        timer.done();

        timer = PerformanceTestUtils.startTimer("str1.equals(str2) succeeds (without intern)");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertTrue(str1.equals(str1B));
        }
        timer.done();

        timer = PerformanceTestUtils.startTimer("str1 == str2 succeeds");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertTrue(str1Interned == str1InternedB);
        }
        timer.done();

        timer = PerformanceTestUtils.startTimer("str1 == str2.intern() succeeds");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertTrue(str1Interned == str1.intern());
        }
        timer.done();

        timer = PerformanceTestUtils.startTimer("str1 == str2.intern() succeeds for interned string");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertTrue(str1Interned == str1InternedB.intern());
        }
        timer.done();

        timer = PerformanceTestUtils.startTimer("str1.equals(str2) = fails (without intern)");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertFalse(str1.equals(str2));
        }
        timer.done();

        timer = PerformanceTestUtils.startTimer("str1 == str2 fails");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertFalse(str1Interned == str2Interned);
        }
        timer.done();

        timer = PerformanceTestUtils.startTimer("str1 == str2.intern() fails");
        for (int i = 0; i < STRING_INTERN_TESTS; i++) {
            assertFalse(str1Interned == str2.intern());
        }
        timer.done();
    }

    /**
     * Generate an array of test strings.
     */
    @Before
    public void generateTestStrings() {
        testStrings.clear();
        random = new Random(123);
        for (int i = 0; i < TEST_STRING_COUNT; i++) {
            testStrings.add(RandomStringUtils.random(10, 0, 0, true, true, null, random));
        }
    }

    /**
     * Measure the speed of {@link OsmPrimitive#put(String, String)}
     */
    @Test
    public void testKeyValuePut() {
        for (double tagNodeRatio : TAG_NODE_RATIOS) {
            int nodeCount = (int) (PUT_RUNS / tagNodeRatio);
            KeyValueDataGenerator generator = OsmDataGenerator.getKeyValue(nodeCount, 0);
            generator.generateDataSet();

            PerformanceTestTimer timer = PerformanceTestUtils
                    .startTimer("OsmPrimitive#put(String, String) with put/node ratio " + tagNodeRatio);

            for (int i = 0; i < PUT_RUNS; i++) {
                String key = generator.randomKey();
                String value = generator.randomValue();
                generator.randomNode().put(key, value);
            }

            timer.done();
        }
    }

    /**
     * Measure the speed of {@link OsmPrimitive#get(String)}
     */
    @Test
    public void testKeyValueGet() {
        for (double tagNodeRatio : TAG_NODE_RATIOS) {
            KeyValueDataGenerator generator = OsmDataGenerator.getKeyValue(tagNodeRatio);
            generator.generateDataSet();

            PerformanceTestTimer timer = PerformanceTestUtils
                    .startTimer("OsmPrimitive#get(String) with tag/node ratio " + tagNodeRatio);
            for (int i = 0; i < GET_RUNS; i++) {
                String key = generator.randomKey();
                // to make comparison easier.
                generator.randomValue();
                generator.randomNode().get(key);
            }
            timer.done();
        }
    }

    /**
     * Measure the speed of {@link OsmPrimitive#getKeys()}
     */
    @Test
    public void testKeyValueGetKeys() {
        for (double tagNodeRatio : TAG_NODE_RATIOS) {
            KeyValueDataGenerator generator = OsmDataGenerator.getKeyValue(tagNodeRatio);
            generator.generateDataSet();

            PerformanceTestTimer timer = PerformanceTestUtils.startTimer("OsmPrimitive#getKeys() with tag/node ratio "
                    + tagNodeRatio);

            for (int i = 0; i < GET_RUNS; i++) {
                // to make comparison easier.
                generator.randomKey();
                generator.randomValue();
                generator.randomNode().getKeys();
            }
            timer.done();
        }
    }

    /**
     * Measure the speed of {@link OsmPrimitive#getKeys()}.get(key)
     */
    @Test
    public void testKeyValueGetKeysGet() {
        for (double tagNodeRatio : TAG_NODE_RATIOS) {
            KeyValueDataGenerator generator = OsmDataGenerator.getKeyValue(tagNodeRatio);
            generator.generateDataSet();

            PerformanceTestTimer timer = PerformanceTestUtils
                    .startTimer("OsmPrimitive#getKeys().get(key) with tag/node ratio " + tagNodeRatio);
            for (int i = 0; i < GET_RUNS; i++) {
                String key = generator.randomKey();
                // to make comparison easier.
                generator.randomValue();
                generator.randomNode().getKeys().get(key);
            }
            timer.done();
        }
    }
}
