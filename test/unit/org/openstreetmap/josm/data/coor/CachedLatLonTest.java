// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import java.text.DecimalFormat;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link CachedLatLon}.
 */
public class CachedLatLonTest {

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of methods {@link CachedLatLon#equals} and {@link CachedLatLon#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(CachedLatLon.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withPrefabValues(DecimalFormat.class, new DecimalFormat("00.0"), new DecimalFormat("00.000"))
            .verify();
    }
}
