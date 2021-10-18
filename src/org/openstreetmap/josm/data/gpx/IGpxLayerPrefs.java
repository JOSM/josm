// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Map;

/**
 * Interface containing the layer preferences.
 * Implemented by GpxLayer and MarkerLayer
 * @since 18287
 */
public interface IGpxLayerPrefs {

    /**
     * The layer specific prefs formerly saved in the preferences, e.g. drawing options.
     * NOT the track specific settings (e.g. color, width)
     * @return Modifiable map
     */
    Map<String, String> getLayerPrefs();

    /**
     * Sets the modified flag to the value.
     * @param value modified flag
     */
    void setModified(boolean value);
}
