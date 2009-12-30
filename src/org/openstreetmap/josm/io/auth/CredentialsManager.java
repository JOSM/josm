// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.PasswordAuthentication;
import java.net.Authenticator.RequestorType;

/**
 * A CredentialManager manages two credentials:
 * <ul>
 *   <li>the credential for {@see RequestorType#SERVER} which is equal to the OSM API credentials
 *   in JOSM</li>
 *   <li>the credential for {@see RequestorType#PROXY} which is equal to the credentials for an
 *   optional HTTP proxy server a user may use</li>
 *  </ul>
 */
public interface CredentialsManager {

    /**
     * Looks up the credentials for a given type.
     *
     * @param the type of service. {@see RequestorType#SERVER} for the OSM API server, {@see RequestorType#PROXY}
     * for a proxy server
     * @return the credentials
     * @throws CredentialsManagerException thrown if a problem occurs in a implementation of this interface
     */
    public PasswordAuthentication lookup(RequestorType requestorType) throws CredentialsManagerException;

    /**
     * Saves the credentials in <code>credentials</code> for the given service type.
     *
     * @param the type of service. {@see RequestorType#SERVER} for the OSM API server, {@see RequestorType#PROXY}
     * for a proxy server
     * @param credentials the credentials
     * @throws CredentialsManagerException thrown if a problem occurs in a implementation of this interface
     */
    public void store(RequestorType requestorType, PasswordAuthentication credentials) throws CredentialsManagerException;

    /**
     *
     * @param requestorType  the type of service. {@see RequestorType#SERVER} for the OSM API server, {@see RequestorType#PROXY}
     * for a proxy server
     * @param noSuccessWithLastResponse true, if the last request with the supplied credentials failed; false otherwise.
     * If true, implementations of this interface are adviced prompt user for new credentials.
     * @throws CredentialsManagerException thrown if a problem occurs in a implementation of this interface

     */
    public CredentialsManagerResponse getCredentials(RequestorType requestorType, boolean noSuccessWithLastResponse) throws CredentialsManagerException;
}
