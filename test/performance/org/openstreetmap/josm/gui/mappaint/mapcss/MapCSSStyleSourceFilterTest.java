// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.PerformanceTestUtils.PerformanceTestTimer;
import org.openstreetmap.josm.data.osm.OsmDataGenerator;
import org.openstreetmap.josm.data.osm.OsmDataGenerator.KeyValueDataGenerator;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests how fast {@link MapCSSStyleSource} finds the right style candidates for one object.
 * @author Michael Zangl
 */
public class MapCSSStyleSourceFilterTest {

    private static final int TEST_RULE_COUNT = 10000;

    private static class CssGenerator {
        StringBuilder sb = new StringBuilder();
        private final KeyValueDataGenerator generator;

        /**
         * Create a new CSS generator.
         * @param generator A generator to get the keys from.
         */
        CssGenerator(KeyValueDataGenerator generator) {
            this.generator = generator;
        }

        private CssGenerator addKeyValueRules(int count) {
            for (int i = 0; i < count; i++) {
                String key = generator.randomKey();
                String value = generator.randomValue();
                addRule("node[\"" + key + "\"=\"" + value + "\"]");
            }
            return this;
        }

        private CssGenerator addKeyRegexpRules(int count) {
            for (int i = 0; i < count; i++) {
                String key = generator.randomKey();
                String value = generator.randomValue();
                value = value.substring(i % value.length());
                addRule("node[\"" + key + "\"=~/.*" + value + ".*/]");
            }
            return this;
        }

        public CssGenerator addHasKeyRules(int count) {
            for (int i = 0; i < count; i++) {
                String key = generator.randomKey();
                addRule("node[\"" + key + "\"]");
            }
            return this;
        }

        public CssGenerator addIsTrueRules(int count) {
            for (int i = 0; i < count; i++) {
                String key = generator.randomKey();
                addRule("node[\"" + key + "\"?]");
            }
            return this;
        }

        private void addRule(String selector) {
            sb.append(selector + " {}\n");
        }

        public String getCss() {
            return sb.toString();
        }
    }

    private static final int APPLY_CALLS = 100000;

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public Timeout globalTimeout = Timeout.seconds(15*60);

    /**
     * Prepare the test.
     */
    @BeforeClass
    public static void createJOSMFixture() {
        JOSMFixture.createPerformanceTestFixture().init(true);
    }

    /**
     * Time how long it takes to evaluate [key=value] rules
     */
    @Test
    public void testKeyValueRules() {
        KeyValueDataGenerator data = OsmDataGenerator.getKeyValue();
        data.generateDataSet();
        CssGenerator css = new CssGenerator(data).addKeyValueRules(TEST_RULE_COUNT);
        runTest(data, css, "only key=value rules");
    }

    /**
     * Time how long it takes to evaluate [key] rules
     */
    @Test
    public void testKeyOnlyRules() {
        KeyValueDataGenerator data = OsmDataGenerator.getKeyValue();
        data.generateDataSet();
        CssGenerator css = new CssGenerator(data).addHasKeyRules(TEST_RULE_COUNT);
        runTest(data, css, "only has key rules");
    }

    /**
     * Time how long it takes to evaluate [key=~...] rules
     */
    @Test
    public void testRegularExpressionRules() {
        KeyValueDataGenerator data = OsmDataGenerator.getKeyValue();
        data.generateDataSet();
        CssGenerator css = new CssGenerator(data).addKeyRegexpRules(TEST_RULE_COUNT);
        runTest(data, css, "regular expressions");
    }

    /**
     * Time how long it takes to evaluate [key?] rules
     */
    @Test
    public void testIsTrueRules() {
        KeyValueDataGenerator data = OsmDataGenerator.getKeyValue();
        data.generateDataSet();
        CssGenerator css = new CssGenerator(data).addIsTrueRules(TEST_RULE_COUNT);
        runTest(data, css, "is true");
    }

    private void runTest(KeyValueDataGenerator data, CssGenerator css, String description) {
        MapCSSStyleSource source = new MapCSSStyleSource(css.getCss());
        PerformanceTestTimer timer = PerformanceTestUtils.startTimer("MapCSSStyleSource#loadStyleSource(...) for " + description);
        source.loadStyleSource();
        timer.done();

        timer = PerformanceTestUtils.startTimer(APPLY_CALLS + "x MapCSSStyleSource#apply(...) for " + description);
        for (int i = 0; i < APPLY_CALLS; i++) {
            MultiCascade mc = new MultiCascade();
            source.apply(mc, data.randomNode(), 1, false);
        }
        timer.done();
    }
}
