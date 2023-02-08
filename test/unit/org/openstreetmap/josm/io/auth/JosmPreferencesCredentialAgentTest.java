// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

/**
 * Test {@link JosmPreferencesCredentialAgent}
 */
class JosmPreferencesCredentialAgentTest implements CredentialsAgentTest<JosmPreferencesCredentialAgent> {

    @Override
    public JosmPreferencesCredentialAgent createAgent() {
        return new JosmPreferencesCredentialAgent();
    }
}
