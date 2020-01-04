// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.layer.gpx.ConvertToDataLayerActionTest;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.io.GpxWriterTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link GpxExtension}
 */
public class GpxExtensionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link GpxExtension#equals} and {@link GpxExtension#hashCode}.
     * @see GpxWriterTest#testExtensions()
     * @see GpxReaderTest#testLayerPrefs()
     * @see ConvertToDataLayerActionTest#testFromTrack()
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        GpxExtensionCollection col = new GpxExtensionCollection();
        col.add("josm", "from-server", "true");
        EqualsVerifier.forClass(GpxExtension.class).usingGetClass()
        .suppress(Warning.NONFINAL_FIELDS)
        .withIgnoredFields("qualifiedName", "parent")
        .withPrefabValues(GpxExtensionCollection.class, new GpxExtensionCollection(), col)
        .verify();
    }

}
