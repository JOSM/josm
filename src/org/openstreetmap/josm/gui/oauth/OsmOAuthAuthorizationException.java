// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

public class OsmOAuthAuthorizationException extends Exception {

    public OsmOAuthAuthorizationException() {
        super();
    }

    public OsmOAuthAuthorizationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public OsmOAuthAuthorizationException(String arg0) {
        super(arg0);
    }

    public OsmOAuthAuthorizationException(Throwable arg0) {
        super(arg0);
    }
}
