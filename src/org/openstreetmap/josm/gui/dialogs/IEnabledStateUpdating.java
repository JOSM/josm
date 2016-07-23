// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

/**
 * To be implemented by actions for which their enabled state depends on another model.
 * @since 10600 (refactoring to new package, functional interface)
 */
@FunctionalInterface
public interface IEnabledStateUpdating {

    /**
     * Called after the layer model has changed.
     */
    void updateEnabledState();
}
