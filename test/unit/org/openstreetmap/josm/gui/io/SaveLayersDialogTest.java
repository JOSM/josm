// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link SaveLayersDialog} class.
 */
public class SaveLayersDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link SaveLayersDialog#confirmSaveLayerInfosOK}.
     */
    @Test
    public void testConfirmSaveLayerInfosOK() {
        final List<SaveLayerInfo> list = Collections.singletonList(new SaveLayerInfo(new OsmDataLayer(new DataSet(), null, null)));
        assertFalse(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel() {
            @Override
            public List<SaveLayerInfo> getLayersWithConflictsAndUploadRequest() {
                return list;
            }
        }));
        assertFalse(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel() {
            @Override
            public List<SaveLayerInfo> getLayersWithoutFilesAndSaveRequest() {
                return list;
            }
        }));
        assertFalse(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel() {
            @Override
            public List<SaveLayerInfo> getLayersWithIllegalFilesAndSaveRequest() {
                return list;
            }
        }));
        assertTrue(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel()));
    }
}
