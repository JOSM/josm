// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Partition tests for {@link SelectAllAction}.
 * This test focuses on the empty dataset partition.
 */
@BasicPreferences
@Main
@Projection
final class SelectAllActionPartitionTest {

    /**
     * Partition P1: Empty dataset
     * Expected behavior: No object should be selected.
     */
    @Test
    void testSelectAllWithEmptyDataSet() {
        DataSet empty = new DataSet();
        MainApplication.getLayerManager()
            .addLayer(new OsmDataLayer(empty, "empty", null));

        assertEquals(0, empty.getSelected().size());

        new SelectAllAction().actionPerformed(null);

        assertEquals(0, empty.getSelected().size());
    }
}
