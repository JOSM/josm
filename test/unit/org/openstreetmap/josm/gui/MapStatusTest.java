// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link MapStatus}
 */
class MapStatusTest {
    /**
     * Unit test of methods {@link MapStatus.StatusTextHistory#equals} and {@link MapStatus.StatusTextHistory#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(MapStatus.StatusTextHistory.class)
            .withIgnoredFields("text").verify();
    }
}
