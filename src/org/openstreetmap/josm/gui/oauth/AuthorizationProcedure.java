// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

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
    MANUALLY;

    /**
     * Returns the translated name of this procedure
     * @return the translated name of this procedure
     */
    public String getText() {
        switch(this) {
        case FULLY_AUTOMATIC:
            return tr("Fully automatic");
        case SEMI_AUTOMATIC:
            return tr("Semi-automatic");
        case MANUALLY:
            return tr("Manual");
        }
        throw new IllegalStateException();
    }

    /**
     * Returns a translated description of this procedure
     * @return a translated description of this procedure
     */
    public String getDescription() {
        switch(this) {
        case FULLY_AUTOMATIC:
            return tr(
                    "<html>Run a fully automatic procedure to get an access token from the OSM website.<br>"
                    + "JOSM accesses the OSM website on behalf of the JOSM user and fully<br>"
                    + "automatically authorizes the user and retrieves an Access Token.</html>"
            );
        case SEMI_AUTOMATIC:
            return tr(
                    "<html>Run a semi-automatic procedure to get an access token from the OSM website.<br>"
                    + "JOSM submits the standards OAuth requests to get a Request Token and an<br>"
                    + "Access Token. It dispatches the user to the OSM website in an external browser<br>"
                    + "to authenticate itself and to accept the request token submitted by JOSM.</html>"
            );
        case MANUALLY:
            return tr(
                    "<html>Enter an Access Token manually if it was generated and retrieved outside<br>"
                    + "of JOSM.</html>"
            );
        }
        throw new IllegalStateException();
    }
}
