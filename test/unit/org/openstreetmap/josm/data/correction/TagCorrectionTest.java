// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.correction;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link TagCorrection}.
 */
class TagCorrectionTest {
    /**
     * Unit test of methods {@link TagCorrection#equals} and {@link TagCorrection#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(TagCorrection.class).usingGetClass().verify();
    }
}
