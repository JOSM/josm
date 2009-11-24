// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTable;

import org.openstreetmap.josm.gui.io.SaveLayersModel.Mode;

class SaveLayersTable extends JTable implements PropertyChangeListener {
    public SaveLayersTable(SaveLayersModel model) {
        super(model, new SaveLayersTableColumnModel());
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(SaveLayersModel.MODE_PROP)) {
            Mode mode = (Mode)evt.getNewValue();
            switch(mode) {
                case EDITING_DATA: setEnabled(true);
                case UPLOADING_AND_SAVING: setEnabled(false);
            }
        }
    }
}
