// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.util.function.Consumer;

/**
 * Interface for OAuth authorization classes
 * @author Taylor Smock
 * @since 18650
 */
public interface IOAuthAuthorization {
    /**
     * Perform the authorization dance
     * @param parameters The OAuth parameters
     * @param consumer The callback for the generated token
     * @param scopes The scopes to ask for
     */
    void authorize(IOAuthParameters parameters, Consumer<IOAuthToken> consumer, Enum<?>... scopes);
}
