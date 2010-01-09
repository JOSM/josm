// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

public class OsmOAuthAuthorisationException extends Exception {

    public OsmOAuthAuthorisationException() {
        super();
    }

    public OsmOAuthAuthorisationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public OsmOAuthAuthorisationException(String arg0) {
        super(arg0);
    }

    public OsmOAuthAuthorisationException(Throwable arg0) {
        super(arg0);
    }
}
