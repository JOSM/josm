// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.io.GpxWriterTest;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link GpxExtension}
 */
class GpxExtensionTest {
    /**
     * Unit test of methods {@link GpxExtension#equals} and {@link GpxExtension#hashCode}.
     * @see GpxWriterTest#testExtensions()
     * @see GpxReaderTest#testLayerPrefs()
     * @see org.openstreetmap.josm.gui.layer.gpx.ConvertToDataLayerActionTest#testFromTrack()
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        GpxExtensionCollection col = new GpxExtensionCollection();
        col.add("josm", "from-server", "true");
        EqualsVerifier.forClass(GpxExtension.class).usingGetClass()
        .suppress(Warning.NONFINAL_FIELDS)
        .withIgnoredFields("qualifiedName", "parent")
        .withPrefabValues(GpxExtensionCollection.class, new GpxExtensionCollection(), col)
        .verify();
    }

}
