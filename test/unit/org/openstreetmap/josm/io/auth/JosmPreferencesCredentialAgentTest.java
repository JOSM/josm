// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import org.openstreetmap.josm.testutils.annotations.HTTP;

/**
 * Test {@link JosmPreferencesCredentialAgent}
 */
@HTTP
class JosmPreferencesCredentialAgentTest implements CredentialsAgentTest<JosmPreferencesCredentialAgent> {

    @Override
    public JosmPreferencesCredentialAgent createAgent() {
        return new JosmPreferencesCredentialAgent();
    }
}
