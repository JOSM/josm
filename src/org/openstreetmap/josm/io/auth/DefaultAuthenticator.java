// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;

/**
 * This is the default authenticator used in JOSM. It delegates lookup of credentials
 * for the OSM API and an optional proxy server to the currently configured {@link CredentialsManager}.
 * @since 2641
 */
public final class DefaultAuthenticator extends Authenticator {
    private static DefaultAuthenticator instance;

    /**
     * Returns the unique instance
     * @return The unique instance
     */
    public static DefaultAuthenticator getInstance() {
        return instance;
    }

    /**
     * Creates the unique instance
     */
    public static void createInstance() {
        instance = new DefaultAuthenticator();
    }

    private final Map<RequestorType, Boolean> credentialsTried = new HashMap<RequestorType, Boolean>();
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
            if (getRequestorType().equals(Authenticator.RequestorType.SERVER) && OsmApi.isUsingOAuth()) {
                // if we are working with OAuth we don't prompt for a password
                return null;
            }
            boolean tried = credentialsTried.get(getRequestorType()) != null;
            CredentialsAgentResponse response = CredentialsManager.getInstance().getCredentials(getRequestorType(), getRequestingHost(), tried);
            if (response == null || response.isCanceled())
                return null;
            credentialsTried.put(getRequestorType(), true);
            return new PasswordAuthentication(response.getUsername(), response.getPassword());
        } catch(CredentialsAgentException e) {
            Main.error(e);
            return null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
