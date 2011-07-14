// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.PasswordAuthentication;
import java.net.Authenticator.RequestorType;

import org.openstreetmap.josm.data.oauth.OAuthToken;

/**
 * A CredentialsAgent manages two credentials:
 * <ul>
 *   <li>the credential for {@see RequestorType#SERVER} which is equal to the OSM API credentials
 *   in JOSM</li>
 *   <li>the credential for {@see RequestorType#PROXY} which is equal to the credentials for an
 *   optional HTTP proxy server a user may use</li>
 *  </ul>
 *
 *  In addition, it manages an OAuth Access Token for accessing the OSM server.
 */
public interface CredentialsAgent {

    /**
     * Looks up the credentials for a given type.
     *
     * @param the type of service. {@see RequestorType#SERVER} for the OSM API server, {@see RequestorType#PROXY}
     * for a proxy server
     * @return the credentials
     * @throws CredentialsAgentException thrown if a problem occurs in a implementation of this interface
     */
    public PasswordAuthentication lookup(RequestorType requestorType) throws CredentialsAgentException;

    /**
     * Saves the credentials in <code>credentials</code> for the given service type.
     *
     * @param the type of service. {@see RequestorType#SERVER} for the OSM API server, {@see RequestorType#PROXY}
     * for a proxy server
     * @param credentials the credentials
     * @throws CredentialsManagerException thrown if a problem occurs in a implementation of this interface
     */
    public void store(RequestorType requestorType, PasswordAuthentication credentials) throws CredentialsAgentException;

    /**
     *
     * @param requestorType  the type of service. {@see RequestorType#SERVER} for the OSM API server, {@see RequestorType#PROXY}
     * for a proxy server
     * @param noSuccessWithLastResponse true, if the last request with the supplied credentials failed; false otherwise.
     * If true, implementations of this interface are advised to prompt the user for new credentials.
     * @throws CredentialsAgentException thrown if a problem occurs in a implementation of this interface

     */
    public CredentialsAgentResponse getCredentials(RequestorType requestorType, boolean noSuccessWithLastResponse) throws CredentialsAgentException;

    /**
     * Lookup the current OAuth Access Token to access the OSM server. Replies null, if no
     * Access Token is currently managed by this CredentialAgent.
     *
     * @return the current OAuth Access Token to access the OSM server.
     * @throws CredentialsAgentException thrown if something goes wrong
     */
    public OAuthToken lookupOAuthAccessToken() throws CredentialsAgentException;

    /**
     * Stores the OAuth Access Token <code>accessToken</code>.
     *
     * @param accessToken the access Token. null, to remove the Access Token.
     * @throws CredentialsAgentException thrown if something goes wrong
     */
    public void storeOAuthAccessToken(OAuthToken accessToken) throws CredentialsAgentException;
}
