// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link MultiMap} class.
 */
public class MultiMapTest {

    /**
     * Unit test of methods {@link MultiMap#equals} and {@link MultiMap#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(MultiMap.class).usingGetClass().verify();
    }
}
