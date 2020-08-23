// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import javax.swing.Icon;
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
     * Returns the checkbox to be added to the UI.
     * @return The checkbox to be added to the UI
     */
    default JCheckBox getCheckBox() {
        return getCheckBox(null);
    }

    /**
     * Returns the checkbox to be added to the UI.
     * @param checkboxChangeListener The listener for checkboxes (may be {@code null})
     * @return The checkbox to be added to the UI
     */
    JCheckBox getCheckBox(ChangeListener checkboxChangeListener);

    /**
     * Returns the icon to be added to the UI.
     * @return The icon to be added to the UI
     */
    default Icon getIcon() {
        return null;
    }

    /**
     * Returns the download task class which will be getting the data.
     * @return The {@link DownloadTask} class which will be getting the data
     */
    Class<? extends AbstractDownloadTask<?>> getDownloadClass();

    /**
     * Determines the last state of the download type.
     * @return The boolean indicating the last state of the download type
     */
    default boolean isEnabled() {
        return getBooleanProperty().get();
    }

    /**
     * Returns the boolean property for this particular download type.
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
