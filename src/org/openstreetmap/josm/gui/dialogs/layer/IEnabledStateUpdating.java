// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

/**
 * To be implemented by actions for which their enabled state depends on another model.
 */
public interface IEnabledStateUpdating {

    /**
     * Called after the layer model has changed.
     */
    void updateEnabledState();
}
