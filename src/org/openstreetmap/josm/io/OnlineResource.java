// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.spi.preferences.Config;

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
    /** Updates of {@link CachedFile} */
    CACHE_UPDATES(tr("Cache updates")),
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
    public String getLocName() {
        return locName;
    }

    /**
     * Replies the offline icon.
     * @return the offline icon
     * @since 17041
     */
    public String getOfflineIcon() {
        switch (this) {
            case OSM_API:
                return /* ICON() */ "offline_osm_api";
            case JOSM_WEBSITE:
                return /* ICON() */ "offline_josm_website";
            case CACHE_UPDATES:
                return /* ICON() */ "offline_cache_updates";
            case CERTIFICATES:
                return /* ICON() */ "offline_certificates";
            case ALL:
                return /* ICON() */ "offline_all";
        }
        return null;
    }

    /**
     * Replies whether the given URL matches this online resource
     * @param url the URL to check
     * @return whether the given URL matches this online resource
     */
    public boolean matches(String url) {
        final String baseUrl;
        switch (this) {
            case ALL:
                return true;
            case OSM_API:
                baseUrl = OsmApi.getOsmApi().getServerUrl();
                break;
            case JOSM_WEBSITE:
                baseUrl = Config.getUrls().getJOSMWebsite();
                break;
            default:
                return false;
        }
        return url.startsWith(baseUrl.substring(baseUrl.indexOf("://")), url.indexOf("://"));
    }
}
