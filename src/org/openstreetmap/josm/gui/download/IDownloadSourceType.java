// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.downloadtasks.AbstractDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.BooleanProperty;

/**
 * An interface to allow arbitrary download sources and types in the primary
 * download window of JOSM
 *
 * @since 16503
 */
public interface IDownloadSourceType {
    /**
     * @return The checkbox to be added to the UI
     */
    default JCheckBox getCheckBox() {
        return getCheckBox(null);
    }

    /**
     * @param checkboxChangeListener The listener for checkboxes (may be {@code null})
     * @return The checkbox to be added to the UI
     */
    JCheckBox getCheckBox(ChangeListener checkboxChangeListener);

    /**
     * @return The {@link DownloadTask} class which will be getting the data
     */
    Class<? extends AbstractDownloadTask<?>> getDownloadClass();

    /**
     * @return The boolean indicating the last state of the download type
     */
    default boolean isEnabled() {
        return getBooleanProperty().get();
    }

    /**
     * @return The boolean property for this particular download type
     */
    BooleanProperty getBooleanProperty();

    /**
     * Check if the area is too large for the current IDownloadSourceType
     *
     * @param bound The bound that will be downloaded
     * @return {@code true} if we definitely cannot download the area;
     */
    boolean isDownloadAreaTooLarge(Bounds bound);
}
