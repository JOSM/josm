// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import org.openstreetmap.josm.testutils.annotations.HTTP;

/**
 * Test class for {@link CredentialsManager}
 */
@HTTP
class CredentialsManagerTest implements CredentialsAgentTest<CredentialsManager> {
    @Override
    public CredentialsManager createAgent() {
        return new CredentialsManager(new JosmPreferencesCredentialAgent());
    }
}
