// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * This is the default authenticator used in JOSM. It delegates lookup of credentials
 * for the OSM API and an optional proxy server to the currently configured {@link CredentialsManager}.
 * @since 2641
 */
public final class DefaultAuthenticator extends Authenticator {
    private static final DefaultAuthenticator INSTANCE = new DefaultAuthenticator();

    /**
     * Returns the unique instance
     * @return The unique instance
     */
    public static DefaultAuthenticator getInstance() {
        return INSTANCE;
    }

    private final Collection<Pair<String, RequestorType>> failedCredentials = new HashSet<>();
    private boolean enabled = true;

    private DefaultAuthenticator() {
    }

    /**
     * Called by the Java HTTP stack when either the OSM API server or a proxy requires authentication.
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (!enabled)
            return null;
        try {
            if (OsmApi.isUsingOAuth()
                    && Objects.equals(OsmApi.getOsmApi().getHost(), getRequestingHost())
                    && RequestorType.SERVER.equals(getRequestorType())) {
                // if we are working with OAuth we don't prompt for a password
                return null;
            }
            final Pair<String, RequestorType> hostTypePair = Pair.create(getRequestingHost(), getRequestorType());
            final boolean hasFailedPreviously = failedCredentials.contains(hostTypePair);
            final CredentialsAgentResponse response = CredentialsManager.getInstance().getCredentials(
                    getRequestorType(), getRequestingHost(), hasFailedPreviously);
            if (response == null || response.isCanceled()) {
                return null;
            }
            if (RequestorType.PROXY.equals(getRequestorType())) {
                // Query user in case this authenticator is called (indicating that the authentication failed) the next time.
                failedCredentials.add(hostTypePair);
            } else {
                // Other parallel requests should not ask the user again, thus wait till this request is finished.
                // In case of invalid authentication, the host is added again to failedCredentials at HttpClient.connect()
                failedCredentials.remove(hostTypePair);
            }
            return new PasswordAuthentication(response.getUsername(), response.getPassword());
        } catch (CredentialsAgentException e) {
            Logging.error(e);
            return null;
        }
    }

    /**
     * Determines whether this authenticator is enabled, i.e.,
     * provides {@link #getPasswordAuthentication() password authentication} via {@link CredentialsManager}.
     * @return whether this authenticator is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enabled/disables this authenticator, i.e., decides whether it
     * should provide {@link #getPasswordAuthentication() password authentication} via {@link CredentialsManager}.
     * @param enabled whether this authenticator should be enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Marks for this host that the authentication failed, i.e.,
     * the {@link CredentialsManager} will show a dialog at the next time.
     * @param host the host to mark
     * @return as per {@link Collection#add(Object)}
     */
    public boolean addFailedCredentialHost(String host) {
        return failedCredentials.add(Pair.create(host, RequestorType.SERVER));
    }

    /**
     * Un-marks the failed authentication attempt for the host
     * @param host the host to un-mark
     * @return as per {@link Collection#remove(Object)}
     */
    public boolean removeFailedCredentialHost(String host) {
        return failedCredentials.remove(Pair.create(host, RequestorType.SERVER));
    }
}
