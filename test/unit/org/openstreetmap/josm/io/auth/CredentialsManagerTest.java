// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.HTTP;

import java.net.Authenticator;
import java.util.List;

/**
 * Test class for {@link CredentialsManager}
 */
@HTTP
class CredentialsManagerTest implements CredentialsAgentTest<CredentialsManager> {
    @Override
    public CredentialsManager createAgent() {
        return new CredentialsManager(new JosmPreferencesCredentialAgent());
    }

    @Test
    public void testMultipleUnsavedHostsLookup() throws CredentialsAgentException {
        final AbstractCredentialsAgent aca = new JosmPreferencesCredentialAgent();
        // A provider that mimics user giving the credentials and choosing not to store them in preferences.
        AbstractCredentialsAgent.setCredentialsProvider((requestorType, agent, response, username, password, host) -> {
            response.setUsername("user" + host);
            response.setPassword("password".toCharArray());
            response.setSaveCredentials(false);
            response.setCanceled(false);
        });
        final CredentialsManager agent = new CredentialsManager(aca);

        String host1 = "example.com";
        String host2 = "example.org";
        for (String host : List.of(host1, host2)) {
            // Try to get credentials after "failure" => provider gives the credentials.
            agent.getCredentials(Authenticator.RequestorType.SERVER, host, true);
        }
        // Both hosts should receive their respective credentials.
        CredentialsAgentResponse response = agent.getCredentials(Authenticator.RequestorType.SERVER, host1, false);
        Assertions.assertEquals("user" + host1, response.getUsername());
        response = agent.getCredentials(Authenticator.RequestorType.SERVER, host2, false);
        Assertions.assertEquals("user" + host2, response.getUsername());
    }
}
