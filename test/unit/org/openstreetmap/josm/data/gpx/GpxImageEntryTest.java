// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link GpxImageEntry} class.
 */
class GpxImageEntryTest {
    /**
     * Unit test of methods {@link GpxImageEntry#equals} and {@link GpxImageEntry#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(GpxImageEntry.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withPrefabValues(GpxImageEntry.class, new GpxImageEntry(new File("foo")), new GpxImageEntry(new File("bar")))
            .verify();
    }
}
