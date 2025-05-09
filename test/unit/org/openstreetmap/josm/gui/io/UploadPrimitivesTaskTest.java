// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.UploadStrategySpecification;

/**
 * Unit tests of {@link UploadPrimitivesTask} class.
 */
class UploadPrimitivesTaskTest {
    /**
     * Test of {@link UploadPrimitivesTask#UploadPrimitivesTask}.
     */
    @Test
    void testUploadPrimitivesTask() {
        assertDoesNotThrow(() -> new UploadPrimitivesTask(
                new UploadStrategySpecification(),
                new OsmDataLayer(new DataSet(), null, null),
                new APIDataSet(),
                new Changeset()));
    }
}
