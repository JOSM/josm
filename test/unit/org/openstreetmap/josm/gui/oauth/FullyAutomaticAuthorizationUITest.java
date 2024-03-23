// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link FullyAutomaticAuthorizationUI} class.
 */
@BasicPreferences
class FullyAutomaticAuthorizationUITest {
    /**
     * Unit test of {@link FullyAutomaticAuthorizationUI#FullyAutomaticAuthorizationUI}.
     */
    @ParameterizedTest
    @EnumSource(OAuthVersion.class)
    void testFullyAutomaticAuthorizationUI(OAuthVersion version) {
        assertDoesNotThrow(() -> new FullyAutomaticAuthorizationUI("", MainApplication.worker, version));
    }
}
