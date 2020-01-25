// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AutoFilterManager} class.
 */
public class AutoFilterManagerTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link AutoFilterManager#getTagValuesForPrimitive}.
     */
    @Test
    public void testTagValuesForPrimitive() {
        final TreeSet<String> values = Stream.of(
                OsmUtils.createPrimitive("way level=-4--5"),
                OsmUtils.createPrimitive("way level=-2"),
                OsmUtils.createPrimitive("node level=0"),
                OsmUtils.createPrimitive("way level=1"),
                OsmUtils.createPrimitive("way level=2;3"),
                OsmUtils.createPrimitive("way level=6-9"),
                OsmUtils.createPrimitive("way level=10;12-13"))
                .flatMap(o -> AutoFilterManager.getTagValuesForPrimitive("level", o))
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(new TreeSet<>(Arrays.asList("-5", "-4", "-2", "0", "1", "2", "3", "6", "7", "8", "9", "10", "12", "13")), values);

    }

    /**
     * Unit test of {@link AutoFilterManager#getTagValuesForPrimitive} provides sensible defaults, see #17496.
     */
    @Test
    public void testTagValuesForPrimitivesDefaults() {
        assertNull(getLayer("way foo=bar"));
        assertEquals("1", getLayer("way bridge=yes"));
        assertEquals("1", getLayer("way power=line"));
        assertEquals("-1", getLayer("way tunnel=yes"));
        assertEquals("0", getLayer("way tunnel=building_passage"));
        assertEquals("0", getLayer("way highway=residential"));
        assertEquals("0", getLayer("way railway=rail"));
        assertEquals("0", getLayer("way waterway=canal"));
    }

    private String getLayer(final String assertion) {
        return AutoFilterManager.getTagValuesForPrimitive("layer", OsmUtils.createPrimitive(assertion))
                .findFirst()
                .orElse(null);
    }
}
