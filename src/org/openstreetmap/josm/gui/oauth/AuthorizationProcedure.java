// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

/**
 * The type of procedure to use for retrieving OAuth credentials.
 */
public enum AuthorizationProcedure {
    /**
     * Run a fully automatic procedure to get an access token from the OSM website.
     * JOSM accesses the OSM website on behalf of the JOSM user and interacts
     * with the site using an OSM session, form posting and screen scraping.
     */
    FULLY_AUTOMATIC,

    /**
     * Run a semi-automatic procedure to get an access token from the OSM website.
     * JOSM submits the standards OAuth requests to get a Request Token and an
     * Access Token. It dispatches the user to the OSM website in an external browser
     * to authenticate itself and to accept the request token submitted by JOSM.
     */
    SEMI_AUTOMATIC,

    /**
     * Enter an Access Token manually. The Access Token could have been generated
     * by another JOSM user and sent to the current JOSM user via email, i.e. in order
     * to grant the current OSM user the right download its private GPS traces. Or it could
     * have been generated in a former session and filed away in a secure place.
     */
    MANUALLY
}
