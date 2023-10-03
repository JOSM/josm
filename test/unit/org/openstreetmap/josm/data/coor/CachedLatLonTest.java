// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link CachedLatLon}.
 */
class CachedLatLonTest {
    /**
     * Unit test of methods {@link CachedLatLon#equals} and {@link CachedLatLon#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(CachedLatLon.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withPrefabValues(DecimalFormat.class, new DecimalFormat("00.0"), new DecimalFormat("00.000"))
            .verify();
    }
}
