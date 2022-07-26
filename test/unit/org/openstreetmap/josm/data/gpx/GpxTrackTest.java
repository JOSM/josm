// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ListenerList;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link GpxTrack}.
 */
// Needed for GpxConstants
@BasicPreferences
class GpxTrackTest {
    /**
     * Tests whether the track can read and write colors.
     */
    @Test
    void testColors() {
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
    void testEqualsContract() {
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
