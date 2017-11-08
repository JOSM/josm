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
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Component allowing input os OSM API URL.
 */
public class OsmApiUrlInputPanel extends JPanel {

    /**
     * OSM API URL property key.
     */
    public static final String API_URL_PROP = OsmApiUrlInputPanel.class.getName() + ".apiUrl";

    private final JLabel lblValid = new JLabel();
    private final JLabel lblApiUrl = new JLabel(tr("OSM Server URL:"));
    private final HistoryComboBox tfOsmServerUrl = new HistoryComboBox();
    private transient ApiUrlValidator valOsmServerUrl;
    private JButton btnTest;
    /** indicates whether to use the default OSM URL or not */
    private JCheckBox cbUseDefaultServerUrl;
    private final transient ListProperty SERVER_URL_HISTORY = new ListProperty("osm-server.url-history", Arrays.asList(
            "https://api06.dev.openstreetmap.org/api", "https://master.apis.dev.openstreetmap.org/api"));

    private transient ApiUrlPropagator propagator;

    /**
     * Constructs a new {@code OsmApiUrlInputPanel}.
     */
    public OsmApiUrlInputPanel() {
        build();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Preferences/Connection#ApiUrl"));
    }

    protected JComponent buildDefaultServerUrlPanel() {
        cbUseDefaultServerUrl = new JCheckBox(tr("<html>Use the default OSM server URL (<strong>{0}</strong>)</html>", OsmApi.DEFAULT_API_URL));
        cbUseDefaultServerUrl.addItemListener(new UseDefaultServerUrlChangeHandler());
        cbUseDefaultServerUrl.setFont(cbUseDefaultServerUrl.getFont().deriveFont(Font.PLAIN));
        return cbUseDefaultServerUrl;
    }

    protected final void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // the checkbox for the default UL
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.gridwidth = 4;
        add(buildDefaultServerUrlPanel(), gc);


        // the input field for the URL
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        add(lblApiUrl, gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmServerUrl, gc);
        lblApiUrl.setLabelFor(tfOsmServerUrl);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmServerUrl.getEditorComponent());
        valOsmServerUrl = new ApiUrlValidator(tfOsmServerUrl.getEditorComponent());
        valOsmServerUrl.validate();
        propagator = new ApiUrlPropagator();
        tfOsmServerUrl.addActionListener(propagator);
        tfOsmServerUrl.addFocusListener(propagator);

        gc.gridx = 2;
        gc.weightx = 0.0;
        add(lblValid, gc);

        gc.gridx = 3;
        gc.weightx = 0.0;
        ValidateApiUrlAction actTest = new ValidateApiUrlAction();
        tfOsmServerUrl.getEditorComponent().getDocument().addDocumentListener(actTest);
        btnTest = new JButton(actTest);
        add(btnTest, gc);
    }

    /**
     * Initializes the configuration panel with values from the preferences
     */
    public void initFromPreferences() {
        String url = OsmApi.getOsmApi().getServerUrl();
        tfOsmServerUrl.setPossibleItems(SERVER_URL_HISTORY.get());
        if (OsmApi.DEFAULT_API_URL.equals(url.trim())) {
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
        String oldUrl = OsmApi.getOsmApi().getServerUrl();
        String hmiUrl = getStrippedApiUrl();
        if (cbUseDefaultServerUrl.isSelected() || OsmApi.DEFAULT_API_URL.equals(hmiUrl)) {
            Config.getPref().put("osm-server.url", null);
        } else {
            Config.getPref().put("osm-server.url", hmiUrl);
            tfOsmServerUrl.addCurrentItemToHistory();
            SERVER_URL_HISTORY.put(tfOsmServerUrl.getHistory());
        }
        String newUrl = OsmApi.getOsmApi().getServerUrl();

        // When API URL changes, re-initialize API connection so we may adjust server-dependent settings.
        if (!oldUrl.equals(newUrl)) {
            try {
                OsmApi.getOsmApi().initialize(null);
            } catch (OsmTransferCanceledException | OsmApiInitializationException ex) {
                Logging.warn(ex);
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
        private String lastTestedUrl;

        ValidateApiUrlAction() {
            putValue(NAME, tr("Validate"));
            putValue(SHORT_DESCRIPTION, tr("Test the API URL"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            final String url = getStrippedApiUrl();
            final ApiUrlTestTask task = new ApiUrlTestTask(OsmApiUrlInputPanel.this, url);
            MainApplication.worker.submit(task);
            Runnable r = () -> {
                if (task.isCanceled())
                    return;
                Runnable r1 = () -> {
                    if (task.isSuccess()) {
                        lblValid.setIcon(ImageProvider.get("dialogs", "valid"));
                        lblValid.setToolTipText(tr("The API URL is valid."));
                        lastTestedUrl = url;
                        updateEnabledState();
                    } else {
                        lblValid.setIcon(ImageProvider.get("warning-small"));
                        lblValid.setToolTipText(tr("Validation failed. The API URL seems to be invalid."));
                    }
                };
                SwingUtilities.invokeLater(r1);
            };
            MainApplication.worker.submit(r);
        }

        protected final void updateEnabledState() {
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

    private static class ApiUrlValidator extends AbstractTextComponentValidator {
        ApiUrlValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            if (getComponent().getText().trim().isEmpty())
                return false;
            return Utils.isValidUrl(getComponent().getText().trim());
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
            default: // Do nothing
            }
        }
    }

    class ApiUrlPropagator extends FocusAdapter implements ActionListener {
        protected void propagate() {
            propagate(getStrippedApiUrl());
        }

        protected void propagate(String url) {
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
