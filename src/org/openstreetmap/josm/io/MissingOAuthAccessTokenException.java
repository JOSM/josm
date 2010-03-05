// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;


public class MissingOAuthAccessTokenException extends OsmTransferException{
    public MissingOAuthAccessTokenException() {
        super();
    }

    public MissingOAuthAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingOAuthAccessTokenException(String message) {
        super(message);
    }

    public MissingOAuthAccessTokenException(Throwable cause) {
        super(cause);
    }
}
