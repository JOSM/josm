// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;

/**
 * Unit tests for class {@link ExtensionFileFilter}.
 */
public class ExtensionFileFilterTest {

    /**
     * Unit test of methods {@link ExtensionFileFilter#equals} and {@link ExtensionFileFilter#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ExtensionFileFilter.class).usingGetClass()
            .verify();
    }
}
