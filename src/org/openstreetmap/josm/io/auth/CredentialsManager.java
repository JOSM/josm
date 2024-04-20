// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.awt.Component;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.util.Objects;

import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * CredentialManager is a factory for the single credential agent used.
 * <p>
 * Currently, it defaults to replying an instance of {@link JosmPreferencesCredentialAgent}.
 * @since 2641
 */
public class CredentialsManager implements CredentialsAgent {

    private static volatile CredentialsManager instance;

    /**
     * Replies the single credential agent used in JOSM
     *
     * @return the single credential agent used in JOSM
     */
    public static CredentialsManager getInstance() {
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

    /**
     * Credentials agent factory.
     */
    @FunctionalInterface
    public interface CredentialsAgentFactory {
        /**
         * Returns the credentials agent instance.
         * @return the credentials agent instance
         */
        CredentialsAgent getCredentialsAgent();
    }

    /**
     * Plugins can register a CredentialsAgentFactory, thereby overriding
     * JOSM's default credentials agent.
     * @param agentFactory The Factory that provides the custom CredentialsAgent.
     * Can be null to clear the factory and switch back to default behavior.
     */
    public static void registerCredentialsAgentFactory(CredentialsAgentFactory agentFactory) {
        CredentialsManager.agentFactory = agentFactory;
        CredentialsManager.instance = null;
    }

    /* non-static fields and methods */

    /**
     * The credentials agent doing the real stuff
     */
    private final CredentialsAgent delegate;

    /**
     * Constructs a new {@code CredentialsManager}.
     * @param delegate The credentials agent backing this credential manager. Must not be {@code null}
     */
    public CredentialsManager(CredentialsAgent delegate) {
        CheckParameterUtil.ensureParameterNotNull(delegate, "delegate");
        this.delegate = delegate;
    }

    /**
     * Returns type of credentials agent backing this credentials manager.
     * @return The type of credentials agent
     */
    public final Class<? extends CredentialsAgent> getCredentialsAgentClass() {
        return delegate.getClass();
    }

    /**
     * Returns the username for OSM API
     * @return the username for OSM API
     */
    public String getUsername() {
        return getUsername(OsmApi.getOsmApi().getHost());
    }

    /**
     * Returns the username for a given host
     * @param host The host for which username is wanted
     * @return The username for {@code host}
     */
    public String getUsername(String host) {
        String username = null;
        try {
            PasswordAuthentication auth = lookup(RequestorType.SERVER, host);
            if (auth != null) {
                username = auth.getUserName();
            }
        } catch (CredentialsAgentException ex) {
            Logging.debug(ex);
            return null;
        }
        if (username == null) return null;
        username = username.trim();
        return username.isEmpty() ? null : username;
    }

    @Override
    public PasswordAuthentication lookup(RequestorType requestorType, String host) throws CredentialsAgentException {
        return delegate.lookup(requestorType, host);
    }

    @Override
    public void store(RequestorType requestorType, String host, PasswordAuthentication credentials) throws CredentialsAgentException {
        if (requestorType == RequestorType.SERVER && Objects.equals(OsmApi.getOsmApi().getHost(), host)) {
            String username = credentials.getUserName();
            if (!Utils.isBlank(username)) {
                UserIdentityManager.getInstance().setPartiallyIdentified(username);
            }
        }
        // see #11914: clear cache before we store new value
        purgeCredentialsCache(requestorType);
        delegate.store(requestorType, host, credentials);
    }

    @Override
    public CredentialsAgentResponse getCredentials(RequestorType requestorType, String host, boolean noSuccessWithLastResponse)
            throws CredentialsAgentException {
        CredentialsAgentResponse credentials = delegate.getCredentials(requestorType, host, noSuccessWithLastResponse);
        if (requestorType == RequestorType.SERVER) {
            // see #11914 : Keep UserIdentityManager up to date
            String userName = credentials.getUsername();
            userName = userName == null ? "" : userName.trim();
            if (!Objects.equals(UserIdentityManager.getInstance().getUserName(), userName)) {
                if (userName.isEmpty())
                    UserIdentityManager.getInstance().setAnonymous();
                else
                    UserIdentityManager.getInstance().setPartiallyIdentified(userName);
            }
        }
        return credentials;
    }

    @Override
    public IOAuthToken lookupOAuthAccessToken(String host) throws CredentialsAgentException {
        return delegate.lookupOAuthAccessToken(host);
    }

    @Override
    public void storeOAuthAccessToken(String host, IOAuthToken accessToken) throws CredentialsAgentException {
        delegate.storeOAuthAccessToken(host, accessToken);
    }

    @Override
    public Component getPreferencesDecorationPanel() {
        return delegate.getPreferencesDecorationPanel();
    }

    @Override
    public void purgeCredentialsCache(RequestorType requestorType) {
        delegate.purgeCredentialsCache(requestorType);
    }
}
