// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IUrls;

/**
 * Class that provides URLs values for JOSM.
 * @since 14119
 */
public final class JosmUrls implements IUrls {

    /**
     * The JOSM website URL.
     */
    private static final String JOSM_WEBSITE = "https://josm.openstreetmap.de";

    /**
     * The OSM website URL.
     */
    private static final String OSM_WEBSITE = "https://www.openstreetmap.org";

    /**
     * The OSM wiki URL.
     */
    private static final String OSM_WIKI = "https://wiki.openstreetmap.org";

    /**
     * public URL of the standard OSM API.
     */
    private static final String DEFAULT_API_URL = "https://api.openstreetmap.org/api";

    private JosmUrls() {
        // hide constructor
    }

    private static class InstanceHolder {
        static final JosmUrls INSTANCE = new JosmUrls();
    }

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static JosmUrls getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public String getOSMWebsiteDependingOnSelectedApi() {
        final String api = OsmApi.getOsmApi().getServerUrl();
        if (DEFAULT_API_URL.equals(api)) {
            return getOSMWebsite();
        } else {
            return api.replaceAll("/api$", "");
        }
    }

    @Override
    public String getBaseBrowseUrl() {
        if (Config.getPref() != null)
            return Config.getPref().get("osm-browse.url", getOSMWebsiteDependingOnSelectedApi());
        return getOSMWebsiteDependingOnSelectedApi();
    }

    @Override
    public String getBaseUserUrl() {
        if (Config.getPref() != null)
            return Config.getPref().get("osm-user.url", getOSMWebsiteDependingOnSelectedApi() + "/user");
        return getOSMWebsiteDependingOnSelectedApi() + "/user";
    }

    @Override
    public String getJOSMWebsite() {
        if (Config.getPref() != null)
            return Config.getPref().get("josm.url", JOSM_WEBSITE);
        return JOSM_WEBSITE;
    }

    @Override
    public String getXMLBase() {
        // Always return HTTP (issues reported with HTTPS)
        return "http://josm.openstreetmap.de";
    }

    @Override
    public String getOSMWebsite() {
        if (Config.getPref() != null)
            return Config.getPref().get("osm.url", OSM_WEBSITE);
        return OSM_WEBSITE;
    }

    @Override
    public String getOSMWiki() {
        if (Config.getPref() != null)
            return Config.getPref().get("url.openstreetmap-wiki", OSM_WIKI);
        return OSM_WIKI;
    }

    @Override
    public String getDefaultOsmApiUrl() {
        return DEFAULT_API_URL;
    }
}
