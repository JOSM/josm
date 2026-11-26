// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.io.MissingOAuthAccessTokenException;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.tools.ExceptionUtil;

import mockit.Mock;
import mockit.MockUp;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link ExceptionDialogUtil} class.
 */
@BasicPreferences
class ExceptionDialogUtilTest {
    /**
     * Tests that {@code ExceptionDialogUtil} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(ExceptionDialogUtil.class);
    }

    /**
     * Without an OAuth Access Token the user is offered to open the preferences, where JOSM is authorized or the
     * client id or a token is entered.
     */
    @Test
    void testMissingOAuthAccessTokenOffersThePreferences() {
        assertEquals(1, explainMissingOAuthAccessToken("Open preferences"));
        assertEquals(0, explainMissingOAuthAccessToken("Close"));
    }

    private static int explainMissingOAuthAccessToken(String answer) {
        TestUtils.assumeWorkingJMockit();
        final MissingOAuthAccessTokenException e = MissingOAuthAccessTokenException.clientIdMissing("https://example.org/api");
        final HelpAwareOptionPaneMocker mocker = new HelpAwareOptionPaneMocker(
                Collections.singletonMap(ExceptionUtil.explainMissingOAuthAccessTokenException(e), answer));
        final AtomicBoolean opened = new AtomicBoolean();
        new MockUp<PreferencesAction>() {
            @Mock
            public void run() {
                opened.set(true);
            }
        };
        ExceptionDialogUtil.explainMissingOAuthAccessTokenException(e);
        assertEquals(1, mocker.getInvocationLog().size());
        assertEquals("Authentication failed", mocker.getInvocationLog().get(0)[2]);
        if (opened.get()) {
            assertTrue(answer.startsWith("Open"));
            return 1;
        }
        assertFalse(answer.startsWith("Open"));
        return 0;
    }
}
