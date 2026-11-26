// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth.osm;

/**
 * The possible scopes for OSM
 * @author Taylor Smock
 * @since 18650
 */
public enum OsmScopes {
    /** Read user preferences */
    read_prefs,
    /** Modify user preferences */
    write_prefs,
    /** Write diary posts */
    write_diary,
    /** Modify the map */
    write_api,
    /** Read private GPS traces */
    read_gpx,
    /** Upload GPS traces */
    write_gpx,
    /** Modify notes */
    write_notes;

    /**
     * Get the scopes JOSM requests when it is authorized. An OAuth 2.0 application which JOSM is to use must be
     * registered with at least these.
     * @return the scopes, in the order they are requested
     * @since 19707
     */
    public static OsmScopes[] getJosmScopes() {
        return new OsmScopes[] {read_gpx, write_gpx, read_prefs, write_prefs, write_api, write_notes};
    }
}
