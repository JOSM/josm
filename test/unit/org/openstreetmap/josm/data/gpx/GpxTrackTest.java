// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ListenerList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link GpxTrack}.
 */
public class GpxTrackTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests weather the track can read and write colors.
     */
    @Test
    public void testColors() {
        GpxTrack trk = new GpxTrack(new ArrayList<IGpxTrackSegment>(), new HashMap<>());
        GpxExtensionCollection ext = trk.getExtensions();
        ext.add("gpxd", "color", "#FF0000");
        trk.invalidate();
        assertEquals(trk.getColor(), Color.RED);
        trk.setColor(Color.GREEN);
        assertEquals(trk.getColor(), Color.GREEN);
        trk.invalidate();
        assertEquals(trk.getColor(), Color.GREEN);
        ext.remove("gpxd", "color");
        trk.invalidate();
        assertNull(trk.getColor());
        ext.add("gpxx", "TrackExtension").getExtensions().add("gpxx", "DisplayColor", "Blue");
        trk.invalidate();
        assertEquals(trk.getColor(), Color.BLUE);
        trk.setColor(null);
        assertNull(trk.getColor());
        trk.invalidate();
        assertNull(trk.getColor());
    }

    /**
     * Unit test of methods {@link GpxTrack#equals} and {@link GpxTrack#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        GpxExtensionCollection col = new GpxExtensionCollection();
        col.add("josm", "from-server", "true");
        EqualsVerifier.forClass(GpxTrack.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withPrefabValues(ListenerList.class, ListenerList.create(), ListenerList.create())
            .withPrefabValues(GpxExtensionCollection.class, new GpxExtensionCollection(), col)
            .withIgnoredFields("bounds", "length", "colorCache", "colorFormat", "listeners")
            .verify();
    }
}
