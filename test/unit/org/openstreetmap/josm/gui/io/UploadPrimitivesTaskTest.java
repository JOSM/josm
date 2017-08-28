// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link UploadPrimitivesTask} class.
 */
public class UploadPrimitivesTaskTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link UploadPrimitivesTask#UploadPrimitivesTask}.
     */
    @Test
    public void testUploadPrimitivesTask() {
        assertNotNull(new UploadPrimitivesTask(
                new UploadStrategySpecification(),
                new OsmDataLayer(new DataSet(), null, null),
                null,
                new Changeset()));
    }
}
