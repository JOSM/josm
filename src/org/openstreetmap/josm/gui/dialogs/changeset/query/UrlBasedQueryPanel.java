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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;

import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlException;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * This panel allows to build a changeset query from an URL.
 * @since 2689
 */
public class UrlBasedQueryPanel extends JPanel {

    private final JosmTextField tfUrl = new JosmTextField();
    private final JLabel lblValid = new JLabel();

    /**
     * Constructs a new {@code UrlBasedQueryPanel}.
     */
    public UrlBasedQueryPanel() {
        build();
    }

    protected JPanel buildURLPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 5);
        pnl.add(new JLabel(tr("URL: ")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(tfUrl, gc);
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
        pnl.add(lblValid, gc);
        lblValid.setPreferredSize(new Dimension(20, 20));
        return pnl;
    }

    protected static List<String> getExamples() {
        return Arrays.asList(
                Config.getUrls().getOSMWebsite()+"/history?open=true",
                OsmApi.getOsmApi().getBaseUrl()+"/changesets?open=true");
    }

    protected JPanel buildHelpPanel() {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        HtmlPanel pnl = new HtmlPanel();
        pnl.setText(
                "<html><body>"
                + tr("Please enter or paste an URL to retrieve changesets from the OSM API.")
                + "<p><strong>" + tr("Examples") + "</strong></p>"
                + "<ul>"
                + String.join("", getExamples().stream().map(
                        s -> "<li><a href=\""+s+"\">"+s+"</a></li>").collect(Collectors.toList()))
                + "</ul>"
                + tr("Note that changeset queries are currently always submitted to ''{0}'', regardless of the "
                        + "host, port and path of the URL entered below.", apiUrl)
                        + "</body></html>"
        );
        pnl.getEditorPane().addHyperlinkListener(e -> {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    tfUrl.setText(e.getDescription());
                    tfUrl.requestFocusInWindow();
                }
            });
        return pnl;
    }

    protected final void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 10, 0);
        add(buildHelpPanel(), gc);

        gc.gridy = 1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        add(buildURLPanel(), gc);

        gc.gridy = 2;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gc);
    }

    protected static boolean isValidChangesetQueryUrl(String text) {
        return buildChangesetQuery(text) != null;
    }

    protected static ChangesetQuery buildChangesetQuery(String text) {
        URL url = null;
        try {
            url = new URL(text);
        } catch (MalformedURLException e) {
            return null;
        }
        String path = url.getPath();
        if (path == null || (!path.endsWith("/changesets") && !path.endsWith("/history")))
            return null;

        try {
            return ChangesetQuery.buildFromUrlQuery(url.getQuery());
        } catch (ChangesetQueryUrlException e) {
            Logging.warn(e);
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

    /**
     * Initializes HMI for user input.
     */
    public void startUserInput() {
        tfUrl.requestFocusInWindow();
    }

    /**
     * Validates text entered in the changeset query URL field on the fly
     */
    class ChangetQueryUrlValidator implements DocumentListener {
        protected String getCurrentFeedback() {
            String fb = (String) lblValid.getClientProperty("valid");
            return fb == null ? "none" : fb;
        }

        protected void feedbackValid() {
            if ("valid".equals(getCurrentFeedback()))
                return;
            lblValid.setIcon(ImageProvider.get("misc", "green_check"));
            lblValid.setToolTipText(null);
            lblValid.putClientProperty("valid", "valid");
        }

        protected void feedbackInvalid() {
            if ("invalid".equals(getCurrentFeedback()))
                return;
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
