// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

/**
 * Listener called when proxy settings are updated.
 * @since 6525
 */
public interface ProxyPreferenceListener {

    /**
     * Method called when proxy settings are updated.
     */
    public void proxyPreferenceChanged();
}
