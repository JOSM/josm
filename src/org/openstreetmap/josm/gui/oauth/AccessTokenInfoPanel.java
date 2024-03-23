// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Displays the key of an OAuth Access Token.
 * @since 2746 (modified for OAuth 2 in 18991)
 */
public class AccessTokenInfoPanel extends JPanel {

    private final JosmTextField tfAccessTokenKey = new JosmTextField(null, null, 0, false);
    private final JCheckBox cbSaveAccessTokenInPreferences = new JCheckBox(tr("Save Access Token in preferences"));

    /**
     * Constructs a new {@code AccessTokenInfoPanel}.
     */
    public AccessTokenInfoPanel() {
        build();
    }

    protected final void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // the access token key
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 3, 3);
        add(new JLabel(tr("Access Token Key:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAccessTokenKey, gc);
        tfAccessTokenKey.setEditable(false);

        // the access token secret
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 3, 3);
        add(new JLabel(tr("Access Token Secret:")), gc);

        gc.weightx = 1.0;
        // the checkbox
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        add(cbSaveAccessTokenInPreferences, gc);
        cbSaveAccessTokenInPreferences.setToolTipText(tr(
                "<html>Select to save the Access Token in the JOSM preferences.<br>"
                + "Unselect to use the Access Token in this JOSM session only.</html>"
        ));
        cbSaveAccessTokenInPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());

        // filler - grab the remaining space
        gc.gridx = 0;
        gc.gridy = 3;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.gridwidth = 2;
        add(new JPanel(), gc);
    }

    /**
     * Displays the key and secret in <code>token</code>.
     *
     * @param token the access  token. If null, the content in the info panel is cleared
     */
    public void setAccessToken(IOAuthToken token) {
        if (token == null) {
            tfAccessTokenKey.setText("");
            return;
        }
        if (token instanceof OAuth20Token) {
            tfAccessTokenKey.setText(((OAuth20Token) token).getBearerToken());
        } else {
            throw new JosmRuntimeException("Unknown token type: " + token.getClass());
        }
    }

    public void setSaveToPreferences(boolean saveToPreferences) {
        cbSaveAccessTokenInPreferences.setSelected(saveToPreferences);
    }

    public boolean isSaveToPreferences() {
        return cbSaveAccessTokenInPreferences.isSelected();
    }
}
