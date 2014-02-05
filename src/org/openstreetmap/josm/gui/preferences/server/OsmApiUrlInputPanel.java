// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * Component allowing input os OSM API URL.
 */
public class OsmApiUrlInputPanel extends JPanel {

    /**
     * OSM API URL property key.
     */
    static public final String API_URL_PROP = OsmApiUrlInputPanel.class.getName() + ".apiUrl";

    private JLabel lblValid;
    private JLabel lblApiUrl;
    private JosmTextField tfOsmServerUrl;
    private ApiUrlValidator valOsmServerUrl;
    private SideButton btnTest;
    /** indicates whether to use the default OSM URL or not */
    private JCheckBox cbUseDefaultServerUrl;

    private ApiUrlPropagator propagator;

    protected JComponent buildDefaultServerUrlPanel() {
        cbUseDefaultServerUrl = new JCheckBox(tr("<html>Use the default OSM server URL (<strong>{0}</strong>)</html>", OsmApi.DEFAULT_API_URL));
        cbUseDefaultServerUrl.addItemListener(new UseDefaultServerUrlChangeHandler());
        cbUseDefaultServerUrl.setFont(cbUseDefaultServerUrl.getFont().deriveFont(Font.PLAIN));
        return cbUseDefaultServerUrl;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // the checkbox for the default UL
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 1.0;
        gc.insets = new Insets(0,0,0,0);
        gc.gridwidth  = 4;
        add(buildDefaultServerUrlPanel(), gc);


        // the input field for the URL
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,0,3);
        add(lblApiUrl = new JLabel(tr("OSM Server URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmServerUrl = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmServerUrl);
        valOsmServerUrl = new ApiUrlValidator(tfOsmServerUrl);
        valOsmServerUrl.validate();
        propagator = new ApiUrlPropagator();
        tfOsmServerUrl.addActionListener(propagator);
        tfOsmServerUrl.addFocusListener(propagator);

        gc.gridx = 2;
        gc.weightx = 0.0;
        add(lblValid = new JLabel(), gc);

        gc.gridx = 3;
        gc.weightx = 0.0;
        ValidateApiUrlAction actTest = new ValidateApiUrlAction();
        tfOsmServerUrl.getDocument().addDocumentListener(actTest);
        add(btnTest = new SideButton(actTest), gc);
    }

    /**
     * Constructs a new {@code OsmApiUrlInputPanel}.
     */
    public OsmApiUrlInputPanel() {
        build();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Preferences/Connection#ApiUrl"));
    }

    /**
     * Initializes the configuration panel with values from the preferences
     */
    public void initFromPreferences() {
        String url =  Main.pref.get("osm-server.url", OsmApi.DEFAULT_API_URL);
        if (url.trim().equals(OsmApi.DEFAULT_API_URL)) {
            cbUseDefaultServerUrl.setSelected(true);
            propagator.propagate(OsmApi.DEFAULT_API_URL);
        } else {
            cbUseDefaultServerUrl.setSelected(false);
            tfOsmServerUrl.setText(url);
            propagator.propagate(url);
        }
    }

    /**
     * Saves the values to the preferences
     */
    public void saveToPreferences() {
        String oldUrl = Main.pref.get("osm-server.url", OsmApi.DEFAULT_API_URL);
        String hmiUrl = getStrippedApiUrl();
        if (cbUseDefaultServerUrl.isSelected()) {
            Main.pref.put("osm-server.url", null);
        } else if (hmiUrl.equals(OsmApi.DEFAULT_API_URL)) {
            Main.pref.put("osm-server.url", null);
        } else {
            Main.pref.put("osm-server.url", hmiUrl);
        }
        String newUrl = Main.pref.get("osm-server.url", OsmApi.DEFAULT_API_URL);

        // When API URL changes, re-initialize API connection so we may adjust
        // server-dependent settings.
        if (!oldUrl.equals(newUrl)) {
            try {
                OsmApi.getOsmApi().initialize(null);
            } catch (Exception x) {
                Main.warn(x);
            }
        }
    }

    /**
     * Returns the entered API URL, stripped of leading and trailing white characters.
     * @return the entered API URL, stripped of leading and trailing white characters.
     *         May be an empty string if nothing has been entered. In this case, it means the user wants to use {@link OsmApi#DEFAULT_API_URL}.
     * @see Utils#strip(String)
     * @since 6602
     */
    public final String getStrippedApiUrl() {
        return Utils.strip(tfOsmServerUrl.getText());
    }

    class ValidateApiUrlAction extends AbstractAction implements DocumentListener {
        private String lastTestedUrl = null;

        public ValidateApiUrlAction() {
            putValue(NAME, tr("Validate"));
            putValue(SHORT_DESCRIPTION, tr("Test the API URL"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            final String url = getStrippedApiUrl();
            final ApiUrlTestTask task = new ApiUrlTestTask(OsmApiUrlInputPanel.this, url);
            Main.worker.submit(task);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (task.isCanceled())
                        return;
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (task.isSuccess()) {
                                lblValid.setIcon(ImageProvider.get("dialogs", "valid"));
                                lblValid.setToolTipText(tr("The API URL is valid."));
                                lastTestedUrl = url;
                                updateEnabledState();
                            } else {
                                lblValid.setIcon(ImageProvider.get("warning-small"));
                                lblValid.setToolTipText(tr("Validation failed. The API URL seems to be invalid."));
                            }
                        }
                    };
                    SwingUtilities.invokeLater(r);
                }
            };
            Main.worker.submit(r);
        }

        protected void updateEnabledState() {
            String url = getStrippedApiUrl();
            boolean enabled = !url.isEmpty() && !url.equals(lastTestedUrl);
            if (enabled) {
                lblValid.setIcon(null);
            }
            setEnabled(enabled);
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }

        @Override
        public void insertUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }
    }

    /**
     * Enables or disables the API URL input.
     * @param enabled {@code true} to enable input, {@code false} otherwise
     */
    public void setApiUrlInputEnabled(boolean enabled) {
        lblApiUrl.setEnabled(enabled);
        tfOsmServerUrl.setEnabled(enabled);
        lblValid.setEnabled(enabled);
        btnTest.setEnabled(enabled);
    }

    static private class ApiUrlValidator extends AbstractTextComponentValidator {
        public ApiUrlValidator(JTextComponent tc) throws IllegalArgumentException {
            super(tc);
        }

        @Override
        public boolean isValid() {
            if (getComponent().getText().trim().isEmpty())
                return false;

            try {
                new URL(getComponent().getText().trim());
                return true;
            } catch(MalformedURLException e) {
                return false;
            }
        }

        @Override
        public void validate() {
            if (getComponent().getText().trim().isEmpty()) {
                feedbackInvalid(tr("OSM API URL must not be empty. Please enter the OSM API URL."));
                return;
            }
            if (!isValid()) {
                feedbackInvalid(tr("The current value is not a valid URL"));
            } else {
                feedbackValid(tr("Please enter the OSM API URL."));
            }
        }
    }

    /**
     * Handles changes in the default URL
     */
    class UseDefaultServerUrlChangeHandler implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            switch(e.getStateChange()) {
            case ItemEvent.SELECTED:
                setApiUrlInputEnabled(false);
                propagator.propagate(OsmApi.DEFAULT_API_URL);
                break;
            case ItemEvent.DESELECTED:
                setApiUrlInputEnabled(true);
                valOsmServerUrl.validate();
                tfOsmServerUrl.requestFocusInWindow();
                propagator.propagate();
                break;
            }
        }
    }

    class ApiUrlPropagator extends FocusAdapter implements ActionListener {
        public void propagate() {
            propagate(getStrippedApiUrl());
        }

        public void propagate(String url) {
            firePropertyChange(API_URL_PROP, null, url);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            propagate();
        }

        @Override
        public void focusLost(FocusEvent arg0) {
            propagate();
        }
    }
}
