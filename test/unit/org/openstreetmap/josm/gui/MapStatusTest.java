// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Test {@link MapStatus}
 */
public class MapStatusTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link MapStatus.StatusTextHistory#equals} and {@link MapStatus.StatusTextHistory#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(MapStatus.StatusTextHistory.class)
            .suppress(Warning.ANNOTATION) // FIXME: To remove once https://github.com/jqno/equalsverifier/issues/197 is fixed
            .withIgnoredFields("text").verify();
    }
}
