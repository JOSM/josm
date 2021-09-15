// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyChangeEvent;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.io.SaveLayersModel.Mode;

/**
 * Unit tests of {@link UploadAndSaveProgressRenderer} class.
 */
class UploadAndSaveProgressRendererTest {
    /**
     * Unit test of {@link UploadAndSaveProgressRenderer#UploadAndSaveProgressRenderer}.
     */
    @Test
    void testUploadAndSaveProgressRenderer() {
        JPanel parent = new JPanel();
        UploadAndSaveProgressRenderer r = new UploadAndSaveProgressRenderer();
        parent.add(r);
        r.setCustomText(null);
        r.setIndeterminate(true);
        r.setMaximum(10);
        r.setTaskTitle(null);
        r.setValue(5);
        r.propertyChange(new PropertyChangeEvent(this, "", null, null));
        r.propertyChange(new PropertyChangeEvent(this, SaveLayersModel.MODE_PROP, null, Mode.UPLOADING_AND_SAVING));
        assertTrue(r.isVisible());
        r.propertyChange(new PropertyChangeEvent(this, SaveLayersModel.MODE_PROP, null, Mode.EDITING_DATA));
        assertFalse(r.isVisible());
    }
}
