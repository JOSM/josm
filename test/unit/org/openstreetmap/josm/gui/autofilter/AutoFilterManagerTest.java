// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
     * Unit test of {@link AutoFilterManager#getTagValuesConsumer}.
     */
    @Test
    public void testTagValuesConsumer() {
        Set<String> values = new TreeSet<>();
        Consumer<OsmPrimitive> consumer = AutoFilterManager.getTagValuesConsumer("level", values);
        Arrays.asList(
                OsmUtils.createPrimitive("way level=-4--5"),
                OsmUtils.createPrimitive("way level=-2"),
                OsmUtils.createPrimitive("node level=0"),
                OsmUtils.createPrimitive("way level=1"),
                OsmUtils.createPrimitive("way level=2;3"),
                OsmUtils.createPrimitive("way level=6-9"),
                OsmUtils.createPrimitive("way level=10;12-13")
                ).forEach(consumer);
        assertEquals(new TreeSet<>(Arrays.asList("-5", "-4", "-2", "0", "1", "2", "3", "6", "7", "8", "9", "10", "12", "13")), values);
    }
}
