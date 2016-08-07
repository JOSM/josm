// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link BBox}.
 */
public class BBoxTest {

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of methods {@link BBox#equals} and {@link BBox#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(BBox.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
