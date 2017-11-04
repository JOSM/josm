// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link ImmutableGpxTrack}.
 */
public class ImmutableGpxTrackTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link ImmutableGpxTrack#equals} and {@link ImmutableGpxTrack#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ImmutableGpxTrack.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withIgnoredFields("bounds", "length")
            .verify();
    }
}
