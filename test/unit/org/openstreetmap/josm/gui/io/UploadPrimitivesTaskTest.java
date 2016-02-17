// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link UploadPrimitivesTask} class.
 */
public class UploadPrimitivesTaskTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

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
