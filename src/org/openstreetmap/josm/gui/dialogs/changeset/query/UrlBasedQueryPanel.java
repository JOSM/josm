// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.ImageProvider;


public class UrlBasedQueryPanel extends JPanel {

    private JosmTextField tfUrl;
    private JLabel lblValid;

    protected JPanel buildURLPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets  = new Insets(0,0,0,5);
        pnl.add(new JLabel(tr("URL: ")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(tfUrl = new JosmTextField(), gc);
        tfUrl.getDocument().addDocumentListener(new ChangetQueryUrlValidator());
        tfUrl.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        tfUrl.selectAll();
                    }
                }
        );

        gc.gridx = 2;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(lblValid = new JLabel(), gc);
        lblValid.setPreferredSize(new Dimension(20,20));
        return pnl;
    }

    protected JPanel buildHelpPanel() {
        HtmlPanel pnl = new HtmlPanel();
        pnl.setText(
                "<html><body>"
                + tr("Please enter or paste an URL to retrieve changesets from the OSM API.")
                + "<p><strong>" + tr("Examples") + "</strong></p>"
                + "<ul>"
                + "<li><a href=\""+Main.OSM_WEBSITE+"/browse/changesets?open=true\">"+Main.OSM_WEBSITE+"/browse/changesets?open=true</a></li>"
                + "<li><a href=\"http://api.openstreetmap.org/api/0.6/changesets?open=true\">http://api.openstreetmap.org/api/0.6/changesets?open=true</a></li>"
                + "</ul>"
                + tr("Note that changeset queries are currently always submitted to ''{0}'', regardless of the "
                        + "host, port and path of the URL entered below.", OsmApi.getOsmApi().getBaseUrl())
                        + "</body></html>"
        );
        pnl.getEditorPane().addHyperlinkListener(
                new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                            tfUrl.setText(e.getDescription());
                            tfUrl.requestFocusInWindow();
                        }
                    }
                }
        );
        return pnl;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0,0,10,0);
        add(buildHelpPanel(),gc);

        gc.gridy = 1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        add(buildURLPanel(),gc);

        gc.gridy = 2;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        add(new JPanel(),gc);

    }
    public UrlBasedQueryPanel() {
        build();
    }

    protected boolean isValidChangesetQueryUrl(String text) {
        return buildChangesetQuery(text) != null;
    }

    protected ChangesetQuery buildChangesetQuery(String text) {
        URL url = null;
        try {
            url = new URL(text);
        } catch(MalformedURLException e) {
            return null;
        }
        String path = url.getPath();
        String query = url.getQuery();
        if (path == null || ! path.endsWith("/changesets")) return null;

        try {
            return ChangesetQuery.buildFromUrlQuery(query);
        } catch(ChangesetQueryUrlException e) {
            return null;
        }
    }

    /**
     * Replies the {@link ChangesetQuery} specified in this panel. null, if no valid changeset query
     * is specified.
     *
     * @return the changeset query
     */
    public ChangesetQuery buildChangesetQuery() {
        String value = tfUrl.getText().trim();
        return buildChangesetQuery(value);
    }

    public void startUserInput() {
        tfUrl.requestFocusInWindow();
    }

    /**
     * Validates text entered in the changeset query URL field on the fly
     */
    class ChangetQueryUrlValidator implements DocumentListener {
        protected String getCurrentFeedback() {
            String fb = (String)lblValid.getClientProperty("valid");
            return fb == null ? "none" : fb;
        }
        protected void feedbackValid() {
            if (getCurrentFeedback().equals("valid")) return;
            lblValid.setIcon(ImageProvider.get("dialogs", "valid"));
            lblValid.setToolTipText("");
            lblValid.putClientProperty("valid", "valid");
        }

        protected void feedbackInvalid() {
            if (getCurrentFeedback().equals("invalid")) return;
            lblValid.setIcon(ImageProvider.get("warning-small"));
            lblValid.setToolTipText(tr("This changeset query URL is invalid"));
            lblValid.putClientProperty("valid", "invalid");
        }

        protected void feedbackNone() {
            lblValid.setIcon(null);
            lblValid.putClientProperty("valid", "none");
        }

        protected void validate() {
            String value = tfUrl.getText();
            if (value.trim().isEmpty()) {
                feedbackNone();
                return;
            }
            value = value.trim();
            if (isValidChangesetQueryUrl(value)) {
                feedbackValid();
            } else {
                feedbackInvalid();
            }
        }
        @Override
        public void changedUpdate(DocumentEvent e) {
            validate();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            validate();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            validate();
        }
    }
}
