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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.tools.ImageProvider;

public class OsmApiUrlInputPanel extends JPanel {
    static public final String API_URL_PROP = OsmApiUrlInputPanel.class.getName() + ".apiUrl";

    static private final String defaulturl = "http://api.openstreetmap.org/api";
    private JLabel lblValid;
    private JLabel lblApiUrl;
    private JTextField tfOsmServerUrl;
    private ApiUrlValidator valOsmServerUrl;
    private SideButton btnTest;
    /** indicates whether to use the default OSM URL or not */
    private JCheckBox cbUseDefaultServerUrl;

    protected JPanel buildDefultServerUrlPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,0,3);
        gc.gridwidth  = 1;
        pnl.add(cbUseDefaultServerUrl = new JCheckBox(), gc);
        cbUseDefaultServerUrl.addItemListener(new UseDefaultServerUrlChangeHandler());

        gc.gridx = 1;
        gc.weightx = 1.0;
        JLabel lbl = new JLabel(tr("<html>Use the default OSM server URL (<strong>{0}</strong>)</html>", defaulturl));
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
        pnl.add(lbl, gc);

        return pnl;
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
        add(buildDefultServerUrlPanel(), gc);


        // the input field for the URL
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,0,3);
        add(lblApiUrl = new JLabel(tr("OSM Server URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmServerUrl = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmServerUrl);
        valOsmServerUrl = new ApiUrlValidator(tfOsmServerUrl);
        valOsmServerUrl.validate();
        ApiUrlPropagator propagator = new ApiUrlPropagator();
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

    public OsmApiUrlInputPanel() {
        build();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Preferences/Connection#ApiUrl"));
    }

    /**
     * Initializes the configuration panel with values from the preferences
     */
    public void initFromPreferences() {
        String url =  Main.pref.get("osm-server.url", null);
        if (url == null) {
            cbUseDefaultServerUrl.setSelected(true);
            firePropertyChange(API_URL_PROP, null, defaulturl);
        } else if (url.trim().equals(defaulturl)) {
            cbUseDefaultServerUrl.setSelected(true);
            firePropertyChange(API_URL_PROP, null, defaulturl);
        } else {
            cbUseDefaultServerUrl.setSelected(false);
            tfOsmServerUrl.setText(url);
            firePropertyChange(API_URL_PROP, null, url);
        }
    }

    /**
     * Saves the values to the preferences
     */
    public void saveToPreferences() {
        if (cbUseDefaultServerUrl.isSelected()) {
            Main.pref.put("osm-server.url", null);
        } else if (tfOsmServerUrl.getText().trim().equals(defaulturl)) {
            Main.pref.put("osm-server.url", null);
        } else {
            Main.pref.put("osm-server.url", tfOsmServerUrl.getText().trim());
        }

    }

    class ValidateApiUrlAction extends AbstractAction implements DocumentListener {
        private String lastTestedUrl = null;

        public ValidateApiUrlAction() {
            putValue(NAME, tr("Validate"));
            putValue(SHORT_DESCRIPTION, tr("Test the API URL"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent arg0) {
            final String url = tfOsmServerUrl.getText().trim();
            final ApiUrlTestTask task = new ApiUrlTestTask(OsmApiUrlInputPanel.this, url);
            Main.worker.submit(task);
            Runnable r = new Runnable() {
                public void run() {
                    if (task.isCanceled())
                        return;
                    Runnable r = new Runnable() {
                        public void run() {
                            if (task.isSuccess()) {
                                lblValid.setIcon(ImageProvider.get("dialogs/changeset", "valid"));
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
            boolean enabled =
                !tfOsmServerUrl.getText().trim().equals("")
                && !tfOsmServerUrl.getText().trim().equals(lastTestedUrl);
            if (enabled) {
                lblValid.setIcon(null);
            }
            setEnabled(enabled);
        }

        public void changedUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }

        public void insertUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }

        public void removeUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }
    }

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
            if (getComponent().getText().trim().equals(""))
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
            if (getComponent().getText().trim().equals("")) {
                feedbackInvalid(tr("OSM API URL must not be empty. Please enter the OSM API URL."));
                return;
            }
            if (!isValid()) {
                feedbackInvalid(tr("The current value isn't a valid URL"));
            } else {
                feedbackValid(tr("Please enter the OSM API URL."));
            }
        }
    }

    /**
     * Handles changes in the default URL
     */
    class UseDefaultServerUrlChangeHandler implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            switch(e.getStateChange()) {
            case ItemEvent.SELECTED:
                setApiUrlInputEnabled(false);
                firePropertyChange(API_URL_PROP, null, defaulturl);
                break;
            case ItemEvent.DESELECTED:
                setApiUrlInputEnabled(true);
                valOsmServerUrl.validate();
                tfOsmServerUrl.requestFocusInWindow();
                firePropertyChange(API_URL_PROP, null, tfOsmServerUrl.getText());
                break;
            }
        }
    }

    class ApiUrlPropagator extends FocusAdapter implements ActionListener {
        public void propagate() {
            firePropertyChange(API_URL_PROP, null, tfOsmServerUrl.getText());
        }

        public void actionPerformed(ActionEvent e) {
            propagate();
        }

        @Override
        public void focusLost(FocusEvent arg0) {
            propagate();
        }
    }
}
