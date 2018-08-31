// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

/**
 * Interface for a provider of certain URLs.
 * @since 14119
 */
public interface IUrls {

    /**
     * Returns the OSM website URL depending on the selected OSM API.
     * @return the OSM website URL depending on the selected OSM API
     */
    String getOSMWebsiteDependingOnSelectedApi();

    /**
     * Replies the base URL for browsing information about a primitive.
     * @return the base URL, i.e. https://www.openstreetmap.org
     */
    String getBaseBrowseUrl();

    /**
     * Replies the base URL for browsing information about a user.
     * @return the base URL, i.e. https://www.openstreetmap.org/user
     */
    String getBaseUserUrl();

    /**
     * Returns the JOSM website URL.
     * @return the josm website URL
     */
    String getJOSMWebsite();

    /**
     * Returns the JOSM XML URL.
     * @return the JOSM XML URL
     */
    String getXMLBase();

    /**
     * Returns the OSM website URL.
     * @return the OSM website URL
     */
    String getOSMWebsite();

    /**
     * Returns the OSM wiki URL.
     * @return the OSM wiki URL
     * @since 14208
     */
    String getOSMWiki();

    /**
     * Returns the default OSM API URL.
     * @return the default OSM API URL
     */
    String getDefaultOsmApiUrl();
}
