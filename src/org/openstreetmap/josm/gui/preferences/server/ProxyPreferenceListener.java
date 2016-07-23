// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

/**
 * Listener called when proxy settings are updated.
 * @since 6525
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface ProxyPreferenceListener {

    /**
     * Method called when proxy settings are updated.
     */
    void proxyPreferenceChanged();
}
