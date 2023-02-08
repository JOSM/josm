// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

/**
 * Base OAuth exception
 * @author Taylor Smock
 * @since 18650
 */
public abstract class OAuthException extends Exception {
    OAuthException(Exception cause) {
        super(cause);
    }

    OAuthException(String message) {
        super(message);
    }

    abstract OAuthVersion[] getOAuthVersions();
}
