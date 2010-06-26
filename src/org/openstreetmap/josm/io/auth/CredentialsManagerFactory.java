// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

/**
 * CredentialManagerFactory is a factory for the single credential manager used.
 *
 * Currently, it defaults to replying an instance of {@see JosmPreferencesCredentialManager}.
 *
 */
public class CredentialsManagerFactory {
    private static CredentialsManager instance;

    /**
     * Replies the single credential manager used in JOSM
     *
     * @return the single credential manager used in JOSM
     */
    static public CredentialsManager getCredentialManager() {
        if (instance == null) {
            instance =  new JosmPreferencesCredentialManager();
        }
        return instance;
    }
}
