// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

import java.net.URL;

/**
 * Listener receiving audio playing events.
 * @since 12328
 */
public interface AudioListener {

    /**
     * Called when a new URL is being played.
     * @param playingURL new URL being played
     */
    void playing(URL playingURL);
}
