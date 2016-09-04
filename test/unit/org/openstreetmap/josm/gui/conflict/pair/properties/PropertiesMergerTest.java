// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PropertiesMerger} class.
 */
public class PropertiesMergerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link PropertiesMerger#PropertiesMerger}.
     */
    @Test
    public void testPropertiesMerger() {
        assertNotNull(new PropertiesMerger());
    }
}
