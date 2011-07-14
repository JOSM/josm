// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;

import org.openstreetmap.josm.data.oauth.OAuthToken;

/**
 * CredentialManager is a factory for the single credential agent used.
 *
 * Currently, it defaults to replying an instance of {@see JosmPreferencesCredentialAgent}.
 *
 */
public class CredentialsManager implements CredentialsAgent {
   
    private static CredentialsManager instance;

    /**
     * Replies the single credential agent used in JOSM
     *
     * @return the single credential agent used in JOSM
     */
    static public CredentialsManager getInstance() {
        if (instance == null) {
            CredentialsAgent delegate;
            if (agentFactory == null) {
                delegate = new JosmPreferencesCredentialAgent();
            } else {
                delegate = agentFactory.getCredentialsAgent();
            }
            instance = new CredentialsManager(delegate);
        }
        return instance;
    }
    
    private static CredentialsAgentFactory agentFactory;

    public interface CredentialsAgentFactory {
        CredentialsAgent getCredentialsAgent();
    }
    
    /**
     * Plugins can register a CredentialsAgentFactory, thereby overriding
     * JOSM's default credentials agent.
     */
    public static void registerCredentialsAgentFactory(CredentialsAgentFactory agentFactory) {
        CredentialsManager.agentFactory = agentFactory;
        CredentialsManager.instance = null;
    }

    /*****
     * non-static fields and methods
     */
    
    private CredentialsAgent delegate;

    public CredentialsManager(CredentialsAgent delegate) {
        this.delegate = delegate;
    }

    @Override
    public PasswordAuthentication lookup(RequestorType requestorType) throws CredentialsAgentException {
        return delegate.lookup(requestorType);
    }

    @Override
    public void store(RequestorType requestorType, PasswordAuthentication credentials) throws CredentialsAgentException {
        delegate.store(requestorType, credentials);
    }

    @Override
    public CredentialsAgentResponse getCredentials(RequestorType requestorType, boolean noSuccessWithLastResponse) throws CredentialsAgentException {
        return delegate.getCredentials(requestorType, noSuccessWithLastResponse);
    }

    @Override
    public OAuthToken lookupOAuthAccessToken() throws CredentialsAgentException {
        return delegate.lookupOAuthAccessToken();
    }

    @Override
    public void storeOAuthAccessToken(OAuthToken accessToken) throws CredentialsAgentException {
        delegate.storeOAuthAccessToken(accessToken);
    }
}
