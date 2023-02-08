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
    write_notes
}
