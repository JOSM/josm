// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.awt.Component;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;

import org.openstreetmap.josm.data.oauth.IOAuthToken;

import jakarta.annotation.Nullable;

/**
 * A CredentialsAgent manages two credentials:
 * <ul>
 *   <li>the credential for {@link RequestorType#SERVER} which is equal to the OSM API credentials
 *   in JOSM</li>
 *   <li>the credential for {@link RequestorType#PROXY} which is equal to the credentials for an
 *   optional HTTP proxy server a user may use</li>
 *  </ul>
 *
 *  In addition, it manages an OAuth Access Token for accessing the OSM server.
 */
public interface CredentialsAgent {

    /**
     * Looks up the credentials for a given type.
     *
     * @param requestorType the type of service. {@link RequestorType#SERVER} for the OSM API server, {@link RequestorType#PROXY}
     * for a proxy server
     * @param host the hostname for these credentials
     * @return the credentials
     * @throws CredentialsAgentException if a problem occurs in a implementation of this interface
     */
    PasswordAuthentication lookup(RequestorType requestorType, String host) throws CredentialsAgentException;

    /**
     * Saves the credentials in <code>credentials</code> for the given service type.
     *
     * @param requestorType the type of service. {@link RequestorType#SERVER} for the OSM API server, {@link RequestorType#PROXY}
     * for a proxy server
     * @param host the hostname for these credentials
     * @param credentials the credentials
     * @throws CredentialsAgentException if a problem occurs in a implementation of this interface
     */
    void store(RequestorType requestorType, String host, PasswordAuthentication credentials) throws CredentialsAgentException;

    /**
     * Returns the credentials needed to to access host.
     * @param requestorType the type of service. {@link RequestorType#SERVER} for the OSM API server, {@link RequestorType#PROXY}
     * for a proxy server
     * @param host the hostname for these credentials
     * @param noSuccessWithLastResponse true, if the last request with the supplied credentials failed; false otherwise.
     * If true, implementations of this interface are advised to prompt the user for new credentials.
     * @return the credentials
     * @throws CredentialsAgentException if a problem occurs in a implementation of this interface
     */
    CredentialsAgentResponse getCredentials(RequestorType requestorType, String host, boolean noSuccessWithLastResponse)
            throws CredentialsAgentException;

    /**
     * Lookup the current OAuth Access Token to access the specified server. Replies null, if no
     * Access Token is currently managed by this CredentialAgent.
     *
     * @param host The host to get OAuth credentials for
     * @return the current OAuth Access Token to access the specified server.
     * @throws CredentialsAgentException if something goes wrong
     * @since 18650
     */
    @Nullable
    IOAuthToken lookupOAuthAccessToken(String host) throws CredentialsAgentException;

    /**
     * Stores the OAuth Access Token <code>accessToken</code>.
     *
     * @param host The host the access token is for
     * @param accessToken the access Token. null, to remove the Access Token. This will remove all IOAuthTokens <i>not</i> managed by
     *                    {@link #storeOAuthAccessToken(String, IOAuthToken)}.
     * @throws CredentialsAgentException if something goes wrong
     * @since 18650
     */
    void storeOAuthAccessToken(String host, IOAuthToken accessToken) throws CredentialsAgentException;

    /**
     * Purges the internal credentials cache for the given requestor type.
     * @param requestorType the type of service.
     * {@link RequestorType#PROXY} for a proxy server, {@link RequestorType#SERVER} for other servers.
     * @since 12992
     */
    void purgeCredentialsCache(RequestorType requestorType);

    /**
     * Purges the internal credentials cache for the given requestor type and host.
     * @param requestorType the type of service.
     * @param host the host.
     * {@link RequestorType#PROXY} for a proxy server, {@link RequestorType#SERVER} for other servers.
     */
    default void purgeCredentialsCache(RequestorType requestorType, String host) {
        purgeCredentialsCache(requestorType);
    }

    /**
     * Provide a Panel that is shown below the API password / username fields
     * in the JOSM Preferences. (E.g. a warning that password is saved unencrypted.)
     * @return Panel
     */
    Component getPreferencesDecorationPanel();
}
