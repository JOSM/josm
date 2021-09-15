// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link UploadPrimitivesTask} class.
 */
@BasicPreferences
class UploadPrimitivesTaskTest {
    /**
     * Test of {@link UploadPrimitivesTask#UploadPrimitivesTask}.
     */
    @Test
    void testUploadPrimitivesTask() {
        assertNotNull(new UploadPrimitivesTask(
                new UploadStrategySpecification(),
                new OsmDataLayer(new DataSet(), null, null),
                null,
                new Changeset()));
    }
}
