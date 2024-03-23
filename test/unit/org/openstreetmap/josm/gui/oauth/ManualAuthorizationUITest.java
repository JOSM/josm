// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ManualAuthorizationUI} class.
 */
@BasicPreferences
class ManualAuthorizationUITest {
    /**
     * Unit test of {@link ManualAuthorizationUI#ManualAuthorizationUI}.
     */
    @Test
    void testManualAuthorizationUI() {
        assertDoesNotThrow(() -> new ManualAuthorizationUI("", MainApplication.worker, OAuthVersion.OAuth20));
    }
}
