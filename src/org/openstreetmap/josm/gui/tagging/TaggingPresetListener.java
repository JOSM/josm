// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

/**
 * Notification of tagging presets events.
 * @since 7100
 */
public interface TaggingPresetListener {

    /**
     * Called after list of tagging presets has been modified.
     */
    void taggingPresetsModified();
}
