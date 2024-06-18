// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

/**
 * Interface for layers that can save data to a file.
 * @since 9751
 */
public interface SaveToFile {

    /**
     * Replies the savable state of the layer (i.e. if it can be saved through
     * a "File → Save" dialog).  A layer that implements the
     * {@code SaveToFile} interface must return {@code true}.
     *
     * @return {@code true} if the layer can be saved to a file; {@code false}, otherwise
     */
    boolean isSavable();

    /**
     * Determines if the data managed by this layer needs to be saved to
     * a file. Only replies true if a file is assigned to this layer and
     * if the data managed by this layer has been modified since the last
     * save operation to the file.
     *
     * @return {@code true} if the data managed by this layer needs to be saved to a file; {@code false}, otherwise
     */
    boolean requiresSaveToFile();

    /**
     * Initializes the layer after a successful save of data to a file.
     */
    void onPostSaveToFile();
}
