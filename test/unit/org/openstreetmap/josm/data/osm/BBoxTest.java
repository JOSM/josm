// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link BBox}.
 */
public class BBoxTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
