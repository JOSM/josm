// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is the abstract base class for the three authorisation UIs.
 *
 * @since 2746
 */
public abstract class AbstractAuthorizationUI extends VerticallyScrollablePanel {
    /**
     * The property name for the Access Token property
     */
    static public final String ACCESS_TOKEN_PROP = AbstractAuthorizationUI.class.getName() + ".accessToken";

    private String apiUrl;
    private final AdvancedOAuthPropertiesPanel pnlAdvancedProperties;
    private OAuthToken accessToken;

    protected void fireAccessTokenChanged(OAuthToken oldValue, OAuthToken newValue) {
        firePropertyChange(ACCESS_TOKEN_PROP, oldValue, newValue);
    }

    /**
     * Constructs a new {@code AbstractAuthorizationUI} for the given API URL.
     * @param apiUrl The OSM API URL
     * @since 5422
     */
    public AbstractAuthorizationUI(String apiUrl) {
        pnlAdvancedProperties = new AdvancedOAuthPropertiesPanel();
        setApiUrl(apiUrl);
    }

    /**
     * Replies the URL of the OSM API for which this UI is currently trying to retrieve an OAuth
     * Access Token
     *
     * @return the API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Sets the URL of the OSM API for which this UI is currently trying to retrieve an OAuth
     * Access Token
     *
     * @param apiUrl the api URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        this.pnlAdvancedProperties.setApiUrl(apiUrl);
    }

    /**
     * Replies the panel for entering advanced OAuth parameters (see {@link OAuthParameters})
     *
     * @return the panel for entering advanced OAuth parameters
     * @see #getOAuthParameters()
     */
    protected AdvancedOAuthPropertiesPanel getAdvancedPropertiesPanel() {
        return pnlAdvancedProperties;
    }

    /**
     * Replies the current set of advanced OAuth parameters in this UI
     *
     * @return the current set of advanced OAuth parameters in this UI
     */
    public OAuthParameters getOAuthParameters() {
        return pnlAdvancedProperties.getAdvancedParameters();
    }

    /**
     * Replies the retrieved Access Token. null, if no Access Token was retrieved.
     *
     * @return the retrieved Access Token
     */
    public  OAuthToken getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the current Access Token. This will fire a property change event for {@link #ACCESS_TOKEN_PROP}
     * if the access token has changed
     *
     * @param accessToken the new access token. null, to clear the current access token
     */
    protected void setAccessToken(OAuthToken accessToken) {
        OAuthToken oldValue = this.accessToken;
        this.accessToken = accessToken;
        if (oldValue == null ^ this.accessToken == null) {
            fireAccessTokenChanged(oldValue, this.accessToken);
        } else if (oldValue == null && this.accessToken == null) {
            // no change - don't fire an event
        } else if (! oldValue.equals(this.accessToken)) {
            fireAccessTokenChanged(oldValue, this.accessToken);
        }
    }

    /**
     * Replies true if this UI currently has an Access Token
     *
     * @return true if this UI currently has an Access Token
     */
    public boolean hasAccessToken() {
        return accessToken != null;
    }

    /**
     * Replies whether the user has chosen to save the Access Token in the JOSM
     * preferences or not.
     *
     * @return true if the user has chosen to save the Access Token
     */
    public abstract boolean isSaveAccessTokenToPreferences();

    /**
     * Initializes the authorisation UI with preference values in <code>pref</code>.
     *
     * @param pref the preferences. Must not be null.
     * @throws IllegalArgumentException thrown if pref is null
     */
    public void initFromPreferences(Preferences pref) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        pnlAdvancedProperties.initFromPreferences(pref);
    }
}
