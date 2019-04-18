// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Online resources directly used by JOSM.
 * This does not include websites where user can sometimes be redirected through its web browser,
 * but only those to we establish a connection.
 *
 * @since 7434
 */
public enum OnlineResource {

    /** The OSM API, used for download, upload, history, etc. */
    OSM_API(tr("OSM API")),
    /** The JOSM website, used for startup page, imagery/presets/styles/rules entries, help, etc. */
    JOSM_WEBSITE(tr("JOSM website")),
    /** Various government certificates downloaded on Windows to make https imagery work in some countries */
    CERTIFICATES(tr("Certificates")),
    /** Value used to represent all online resources */
    ALL(tr("All"));

    private final String locName;

    OnlineResource(String locName) {
        this.locName = locName;
    }

    /**
     * Replies the localized name.
     * @return the localized name
     */
    public final String getLocName() {
        return locName;
    }

    /**
     * Ensures resource is not accessed in offline mode.
     * @param downloadString The attempted download string
     * @param resourceString The resource download string that should not be accessed
     * @throws OfflineAccessException if resource is accessed in offline mode, in any protocol
     */
    public final void checkOfflineAccess(String downloadString, String resourceString) {
        if (NetworkManager.isOffline(this) && downloadString
                .startsWith(resourceString.substring(resourceString.indexOf("://")), downloadString.indexOf("://"))) {
            throw new OfflineAccessException(tr("Unable to access ''{0}'': {1} not available (offline mode)", downloadString, getLocName()));
        }
    }
}
