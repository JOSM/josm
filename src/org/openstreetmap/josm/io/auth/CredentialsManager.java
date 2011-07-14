// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

/**
 * CredentialManager is a factory for the single credential agent used.
 *
 * Currently, it defaults to replying an instance of {@see JosmPreferencesCredentialAgent}.
 *
 */
public class CredentialsManager {
    private static CredentialsAgent instance;

    /**
     * Replies the single credential agent used in JOSM
     *
     * @return the single credential agent used in JOSM
     */
    static public CredentialsAgent getInstance() {
        if (instance == null) {
            instance =  new JosmPreferencesCredentialAgent();
        }
        return instance;
    }
}
