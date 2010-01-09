// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;

/**
 * This is the default authenticator used in JOSM. It delegates lookup of credentials
 * for the OSM API and an optional proxy server to the currently configured
 * {@see CredentialsManager}.
 *
 */
public  class DefaultAuthenticator extends Authenticator {
    private static final Logger logger = Logger.getLogger(DefaultAuthenticator.class.getName());
    private static DefaultAuthenticator instance;

    public static DefaultAuthenticator getInstance() {
        return instance;
    }

    public static void createInstance(CredentialsManager credentialManager) {
        instance = new DefaultAuthenticator(credentialManager);
    }

    private CredentialsManager credentialManager;
    private final Map<RequestorType, Boolean> credentialsTried = new HashMap<RequestorType, Boolean>();
    private boolean enabled = true;

    /**
     *
     * @param credentialManager the credential manager
     */
    private DefaultAuthenticator(CredentialsManager credentialManager) {
        this.credentialManager = credentialManager;
    }

    /**
     * Called by the Java http stack when either the OSM API server or a proxy requires
     * authentication.
     *
     */
    @Override protected PasswordAuthentication getPasswordAuthentication() {
        if (!enabled)
            return null;
        try {
            if (getRequestorType().equals(Authenticator.RequestorType.SERVER)) {
                // if we are working with OAuth we don't prompt for a password
                //
                String authMethod = Main.pref.get("osm-server.auth-method", "basic");
                if (authMethod.equals("oauth"))
                    return null;
            }
            boolean tried = credentialsTried.get(getRequestorType()) != null;
            CredentialsManagerResponse response = credentialManager.getCredentials(getRequestorType(), tried);
            if (response == null || response.isCanceled())
                return null;
            credentialsTried.put(getRequestorType(), true);
            return new PasswordAuthentication(response.getUsername(), response.getPassword());
        } catch(CredentialsManagerException e) {
            e.printStackTrace();
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
