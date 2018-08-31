// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link GpxImageEntry} class.
 */
public class GpxImageEntryTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link GpxImageEntry#equals} and {@link GpxImageEntry#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(GpxImageEntry.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withPrefabValues(GpxImageEntry.class, new GpxImageEntry(new File("foo")), new GpxImageEntry(new File("bar")))
            .verify();
    }
}
