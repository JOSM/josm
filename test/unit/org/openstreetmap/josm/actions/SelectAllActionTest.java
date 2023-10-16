// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link SelectAllAction}.
 */
@BasicPreferences
@Main
@Projection
final class SelectAllActionTest {
    /**
     * Unit test of {@link SelectAllAction#actionPerformed} method.
     */
    @Test
    void testActionPerformed() {
        SelectByInternalPointActionTest.initDataSet();
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();

        assertEquals(0, ds.getSelected().size());
        new SelectAllAction().actionPerformed(null);
        assertEquals(6, ds.getSelected().size());
    }
}
