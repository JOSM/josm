// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.oauth.IOAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.data.oauth.osm.OsmScopes;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Unit tests of {@link AdvancedOAuthPropertiesPanel}.
 */
@BasicPreferences
class AdvancedOAuthPropertiesPanelTest {
    private static final String THIRD_PARTY = "https://nonprod-mapops.example.org/api";
    private static final String THIRD_PARTY_HOST = "nonprod-mapops.example.org";
    private static final String OHM = "https://api.openhistoricalmap.org/api";
    private static final String OHM_HOST = "api.openhistoricalmap.org";

    @BeforeEach
    void setUp() {
        // the default parameters would otherwise be looked up with an RFC 8414 request
        NetworkManager.setOffline(OnlineResource.ALL);
    }

    @AfterEach
    void tearDown() {
        NetworkManager.setOnline(OnlineResource.ALL);
        // the preferences are static state shared with the other tests of this class
        for (String host : new String[] {THIRD_PARTY_HOST, OHM_HOST}) {
            Config.getPref().put("oauth.access-token.parameters.OAuth20." + host, null);
            Config.getPref().put("oauth.settings.use-default." + host, null);
        }
    }

    private static void waitForWorkerAndEdt() throws ExecutionException, InterruptedException {
        MainApplication.worker.submit(() -> { }).get();
        GuiHelper.runInEDTAndWait(() -> { });
    }

    /**
     * Each server has OAuth settings of its own: the panel shows those of the server whose API URL it is given,
     * and saves them for that server only.
     * @throws ExecutionException if waiting for the worker fails
     * @throws InterruptedException if waiting for the worker is interrupted
     */
    @Test
    void testSettingsFollowTheServer() throws ExecutionException, InterruptedException {
        OAuthParameters.setUseDefault(THIRD_PARTY, false);
        new OAuth20Parameters("entered-client-id", null, THIRD_PARTY, THIRD_PARTY, "http://127.0.0.1:8111/oauth_authorization")
                .rememberPreferences();
        final String entered = Config.getPref().get("oauth.access-token.parameters.OAuth20." + THIRD_PARTY_HOST, null);

        final AdvancedOAuthPropertiesPanel panel = new AdvancedOAuthPropertiesPanel(OAuthVersion.OAuth20);
        panel.initialize(THIRD_PARTY);
        waitForWorkerAndEdt();
        assertFalse(panel.isUseDefaultSettings());
        assertEquals("entered-client-id", panel.getEnteredClientId());

        panel.setApiUrl(OHM);
        waitForWorkerAndEdt();
        assertTrue(panel.isUseDefaultSettings(), "the settings of the other server were kept");
        assertEquals("Hl5yIhFS-Egj6aY7A35ouLOuZl0EHjj8JJQQ46IO96E", panel.getEnteredClientId());
        panel.rememberPreferences();
        assertTrue(OAuthParameters.isUseDefault(OHM));
        assertFalse(OAuthParameters.isUseDefault(THIRD_PARTY), "saved for another server");
        assertEquals(entered, Config.getPref().get("oauth.access-token.parameters.OAuth20." + THIRD_PARTY_HOST, null));

        panel.setApiUrl(THIRD_PARTY);
        waitForWorkerAndEdt();
        assertFalse(panel.isUseDefaultSettings());
        assertEquals("entered-client-id", panel.getEnteredClientId());
    }

    /**
     * The OAuth parameters are shown when the user enters them, that is when the default settings are not used.
     * The access token is not one of them: it can be pasted whichever settings are used.
     * @throws ReflectiveOperationException if a field cannot be accessed
     * @throws ExecutionException if waiting for the worker fails
     * @throws InterruptedException if waiting for the worker is interrupted
     */
    @Test
    void testParametersAreShownWhenEntered() throws ReflectiveOperationException, ExecutionException, InterruptedException {
        final AdvancedOAuthPropertiesPanel panel = new AdvancedOAuthPropertiesPanel(OAuthVersion.OAuth20);
        panel.initialize(THIRD_PARTY);
        waitForWorkerAndEdt();
        final JComponent clientId = getField(panel, "tfClientId");
        final JComponent accessToken = getField(panel, "tfAccessToken");
        assertTrue(panel.isUseDefaultSettings());
        assertFalse(clientId.isVisible());
        assertTrue(accessToken.isVisible());

        ((JCheckBox) getField(panel, "cbUseDefaults")).setSelected(false);
        assertTrue(clientId.isVisible());
        assertTrue(accessToken.isVisible());

        OAuthParameters.setUseDefault(THIRD_PARTY, false);
        final AdvancedOAuthPropertiesPanel entered = new AdvancedOAuthPropertiesPanel(OAuthVersion.OAuth20);
        entered.initialize(THIRD_PARTY);
        assertTrue((getField(entered, "tfClientId")).isVisible(), "the parameters are entered for this server");
    }

    /**
     * Each field explains what it holds, and so does its label; the client id and the access token list the
     * permissions JOSM needs, which the application has to be registered with.
     */
    @Test
    void testToolTips() {
        final AdvancedOAuthPropertiesPanel panel = new AdvancedOAuthPropertiesPanel(OAuthVersion.OAuth20);
        int labels = 0;
        for (Component c : panel.getComponents()) {
            if (c instanceof JosmTextField || c instanceof JCheckBox) {
                assertNotNull(((JComponent) c).getToolTipText(), c::toString);
            } else if (c instanceof JLabel) {
                final JComponent field = (JComponent) ((JLabel) c).getLabelFor();
                assertNotNull(field, ((JLabel) c)::getText);
                assertEquals(field.getToolTipText(), ((JLabel) c).getToolTipText());
                labels++;
            }
        }
        assertEquals(6, labels);
        for (String name : new String[] {"tfClientId", "tfAccessToken"}) {
            final String toolTip = assertDoesNotThrow(() -> getField(panel, name)).getToolTipText();
            for (OsmScopes scope : OsmScopes.getJosmScopes()) {
                assertTrue(toolTip.contains(scope.name()), () -> name + " does not list " + scope + ": " + toolTip);
            }
        }
    }

    /**
     * White space copied along with the values does not become part of them. Non-regression test: a client secret
     * pasted with a trailing space was sent as it was, and the server refused the token request with
     * "invalid_client".
     * @throws ReflectiveOperationException if a field cannot be accessed
     * @throws ExecutionException if waiting for the worker fails
     * @throws InterruptedException if waiting for the worker is interrupted
     */
    @Test
    void testEnteredValuesAreTrimmed() throws ReflectiveOperationException, ExecutionException, InterruptedException {
        final AdvancedOAuthPropertiesPanel panel = new AdvancedOAuthPropertiesPanel(OAuthVersion.OAuth20);
        panel.initialize(THIRD_PARTY);
        waitForWorkerAndEdt();
        ((JCheckBox) getField(panel, "cbUseDefaults")).setSelected(false);
        ((JTextField) getField(panel, "tfClientId")).setText(" a-client-id\t");
        ((JTextField) getField(panel, "tfClientSecret")).setText("a-client-secret ");
        ((JTextField) getField(panel, "tfRedirectUri")).setText(" http://127.0.0.1:8111/oauth_authorization");
        ((JTextField) getField(panel, "tfAccessTokenURL")).setText("https://nonprod-mapops.example.org/oauth2/token\n");
        ((JTextField) getField(panel, "tfAuthoriseURL")).setText("  https://nonprod-mapops.example.org/oauth2/authorize");

        final IOAuthParameters parameters = panel.getAdvancedParameters();
        assertEquals("a-client-id", parameters.getClientId());
        assertEquals("a-client-secret", parameters.getClientSecret());
        assertEquals("http://127.0.0.1:8111/oauth_authorization", parameters.getRedirectUri());
        assertEquals("https://nonprod-mapops.example.org/oauth2/token", parameters.getAccessTokenUrl());
        assertEquals("https://nonprod-mapops.example.org/oauth2/authorize", parameters.getAuthorizationUrl());
        assertEquals("a-client-id", panel.getEnteredClientId());
    }

    private static JComponent getField(AdvancedOAuthPropertiesPanel panel, String name) throws ReflectiveOperationException {
        final Field field = AdvancedOAuthPropertiesPanel.class.getDeclaredField(name);
        ReflectionUtils.setObjectsAccessible(field);
        return (JComponent) field.get(panel);
    }

    /**
     * Saving with "use default settings" ticked keeps the settings entered earlier, for when they are needed again.
     * @throws ExecutionException if waiting for the worker fails
     * @throws InterruptedException if waiting for the worker is interrupted
     */
    @Test
    void testUseDefaultKeepsTheEnteredSettings() throws ExecutionException, InterruptedException {
        new OAuth20Parameters("entered-client-id", null, THIRD_PARTY, THIRD_PARTY, "http://127.0.0.1:8111/oauth_authorization")
                .rememberPreferences();
        final AdvancedOAuthPropertiesPanel panel = new AdvancedOAuthPropertiesPanel(OAuthVersion.OAuth20);
        panel.initialize(THIRD_PARTY);
        waitForWorkerAndEdt();
        assertTrue(panel.isUseDefaultSettings());
        panel.rememberPreferences();
        waitForWorkerAndEdt();

        OAuthParameters.setUseDefault(THIRD_PARTY, false);
        assertEquals("entered-client-id", OAuthParameters.createFromApiUrl(THIRD_PARTY, OAuthVersion.OAuth20).getClientId());
    }
}
