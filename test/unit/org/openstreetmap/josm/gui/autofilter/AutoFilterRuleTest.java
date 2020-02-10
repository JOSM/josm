// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.NoSuchElementException;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AutoFilterRule} class.
 */
public class AutoFilterRuleTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link AutoFilterRule#getTagValuesForPrimitive}.
     */
    @Test
    public void testTagValuesForPrimitive() {
        final AutoFilterRule level = AutoFilterRule.getDefaultRule("level").orElseThrow(NoSuchElementException::new);
        assertTagValuesForPrimitive(level, "way level=-4--5", -5, -4);
        assertTagValuesForPrimitive(level, "way level=-2", -2);
        assertTagValuesForPrimitive(level, "node level=0", 0);
        assertTagValuesForPrimitive(level, "way level=1", 1);
        assertTagValuesForPrimitive(level, "way level=2;3", 2, 3);
        assertTagValuesForPrimitive(level, "way level=6-9", 6, 7, 8, 9);
        assertTagValuesForPrimitive(level, "way level=10;12-13", 10, 12, 13);
    }

    /**
     * Unit test of {@link AutoFilterRule#getTagValuesForPrimitive} to deal with {@code %} of key {@code incline}.
     */
    @Test
    public void testTagValuesForPrimitiveInclineUnit() {
        final AutoFilterRule incline = AutoFilterRule.getDefaultRule("incline").orElseThrow(NoSuchElementException::new);
        assertTagValuesForPrimitive(incline, "way incline=up");
        assertTagValuesForPrimitive(incline, "way incline=20", 20);
        assertTagValuesForPrimitive(incline, "way incline=20%", 20);
    }

    /**
     * Unit test of {@link AutoFilterRule#getTagValuesForPrimitive} provides sensible defaults, see #17496.
     */
    @Test
    public void testTagValuesForPrimitivesDefaults() {
        final AutoFilterRule layer = AutoFilterRule.getDefaultRule("layer").orElseThrow(NoSuchElementException::new);
        assertTagValuesForPrimitive(layer, "way foo=bar");
        assertTagValuesForPrimitive(layer, "way bridge=yes", 1);
        assertTagValuesForPrimitive(layer, "way power=line", 1);
        assertTagValuesForPrimitive(layer, "way tunnel=yes", -1);
        assertTagValuesForPrimitive(layer, "way tunnel=building_passage", 0);
        assertTagValuesForPrimitive(layer, "way highway=residential", 0);
        assertTagValuesForPrimitive(layer, "way railway=rail", 0);
        assertTagValuesForPrimitive(layer, "way waterway=canal", 0);
    }

    private void assertTagValuesForPrimitive(AutoFilterRule rule, String assertion, int... expected) {
        final OsmPrimitive primitive = OsmUtils.createPrimitive(assertion);
        final int[] actual = rule.getTagValuesForPrimitive(primitive).toArray();
        assertArrayEquals(expected, actual);
    }

    /**
     * Unit test of {@link AutoFilterRule#formatValue}
     */
    @Test
    public void testValueFormatter() {
        final AutoFilterRule voltage = AutoFilterRule.getDefaultRule("voltage").orElseThrow(NoSuchElementException::new);
        assertEquals("230V", voltage.formatValue(230));
        assertEquals("1kV", voltage.formatValue(1000));
        assertEquals("15kV", voltage.formatValue(15000));
        assertEquals("380kV", voltage.formatValue(380000));
    }
}
