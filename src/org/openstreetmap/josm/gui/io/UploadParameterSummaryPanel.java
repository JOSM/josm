// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A panel that displays a summary of data the user is about to upload
 * <p>
 * FIXME this class should extend HtmlPanel instead (duplicated code in here)
 */
public class UploadParameterSummaryPanel extends JPanel implements HyperlinkListener, PropertyChangeListener {
    private transient UploadStrategySpecification spec = new UploadStrategySpecification();
    private int numObjects;
    private JMultilineLabel jepMessage;
    private JLabel lblWarning;

    private transient Changeset selectedChangeset;
    private boolean closeChangesetAfterNextUpload;
    private transient ConfigurationParameterRequestHandler configHandler;

    /**
     * Constructs a new {@code UploadParameterSummaryPanel}.
     */
    public UploadParameterSummaryPanel() {
        build();
        updateSummary();
    }

    protected String buildChangesetSummary() {
        StringBuilder msg = new StringBuilder(96);
        if (selectedChangeset == null || selectedChangeset.isNew()) {
            msg.append(tr("Objects are uploaded to a <strong>new changeset</strong>."));
        } else {
            msg.append(tr("Objects are uploaded to the <strong>open changeset</strong> {0} with upload comment ''{1}''.",
                    selectedChangeset.getId(),
                    selectedChangeset.getComment()
            ));
        }
        msg.append(' ');
        if (closeChangesetAfterNextUpload) {
            msg.append(tr("The changeset is going to be <strong>closed</strong> after this upload"));
        } else {
            msg.append(tr("The changeset is <strong>left open</strong> after this upload"));
        }
        msg.append(" (<a href=\"urn:changeset-configuration\">").append(tr("configure changeset")).append("</a>)");
        return msg.toString();
    }

    protected String buildStrategySummary() {
        if (spec == null)
            return "";
        // check whether we can use one changeset only or whether we have to use multiple changesets
        //
        boolean useOneChangeset = true;
        Capabilities capabilities = OsmApi.getOsmApi().getCapabilities();
        int maxChunkSize = capabilities != null ? capabilities.getMaxChangesetSize() : -1;
        if (maxChunkSize > 0 && numObjects > maxChunkSize) {
            useOneChangeset = false;
        }

        int numRequests = spec.getNumRequests(numObjects);
        String msg = null;
        if (useOneChangeset) {
            lblWarning.setVisible(false);
            if (numRequests == 0) {
                msg = trn(
                        "Uploading <strong>{0} object</strong> to <strong>1 changeset</strong>",
                        "Uploading <strong>{0} objects</strong> to <strong>1 changeset</strong>",
                        numObjects, numObjects
                );
            } else if (numRequests == 1) {
                msg = trn(
                        "Uploading <strong>{0} object</strong> to <strong>1 changeset</strong> using <strong>1 request</strong>",
                        "Uploading <strong>{0} objects</strong> to <strong>1 changeset</strong> using <strong>1 request</strong>",
                        numObjects, numObjects
                );
            } else if (numRequests > 1) {
                msg = tr("Uploading <strong>{0} objects</strong> to <strong>1 changeset</strong> using <strong>{1} requests</strong>",
                        numObjects, numRequests);
            }
            msg = msg + " (<a href=\"urn:advanced-configuration\">" + tr("advanced configuration") + "</a>)";
        } else {
            lblWarning.setVisible(true);
            if (numRequests == 0) {
                msg = tr("{0} objects exceed the max. allowed {1} objects in a changeset on the server ''{2}''. " +
                        "Please <a href=\"urn:advanced-configuration\">configure</a> how to proceed with <strong>multiple changesets</strong>",
                        numObjects, maxChunkSize, OsmApi.getOsmApi().getBaseUrl());
            } else if (numRequests > 1) {
                msg = tr("Uploading <strong>{0} objects</strong> to <strong>multiple changesets</strong> using <strong>{1} requests</strong>",
                        numObjects, numRequests);
                msg = msg + " (<a href=\"urn:advanced-configuration\">" + tr("advanced configuration") + "</a>)";
            }
        }
        return msg;
    }

    protected void build() {
        jepMessage = new JMultilineLabel("");
        jepMessage.addHyperlinkListener(this);

        setLayout(new BorderLayout());
        add(jepMessage, BorderLayout.CENTER);
        lblWarning = new JLabel("");
        lblWarning.setVisible(false);
        lblWarning.setLabelFor(jepMessage);
        lblWarning.setIcon(ImageProvider.get("warning-small"));
        lblWarning.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(lblWarning, BorderLayout.NORTH);
        add(pnl, BorderLayout.WEST);
    }

    public void setConfigurationParameterRequestListener(ConfigurationParameterRequestHandler handler) {
        this.configHandler = handler;
    }

    /**
     * Sets the {@link UploadStrategySpecification} the user chose
     * @param spec The specification to display
     */
    public void setUploadStrategySpecification(UploadStrategySpecification spec) {
        this.spec = spec;
        updateSummary();
    }

    /**
     * Sets the number of objects that will be uploaded
     * @param numObjects The number to display
     */
    public void setNumObjects(int numObjects) {
        this.numObjects = numObjects;
        updateSummary();
    }

    /**
     * Display that the changeset will be closed after the upload
     * @param value <code>true</code> if it will be closed
     */
    public void setCloseChangesetAfterNextUpload(boolean value) {
        this.closeChangesetAfterNextUpload = value;
        updateSummary();
    }

    protected void updateSummary() {
        final String server = Optional.of(OsmApi.getOsmApi().getServerUrl())
                .filter(url -> !Config.getUrls().getDefaultOsmApiUrl().equals(url))
                .map(url -> tr("… to server: <strong>{0}</strong>", url))
                .orElse("");
        jepMessage.setText("<html>"
                + "<br>"
                + buildChangesetSummary()
                + "<br><br>"
                + buildStrategySummary()
                + "<br>"
                + server
                + "</html>");
    }

    /* --------------------------------------------------------------------- */
    /* Interface HyperlinkListener
    /* --------------------------------------------------------------------- */
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            String desc = e.getDescription();
            if (desc == null || configHandler == null)
                return;
            if ("urn:changeset-configuration".equals(desc)) {
                configHandler.handleChangesetConfigurationRequest();
            } else if ("urn:advanced-configuration".equals(desc)) {
                configHandler.handleUploadStrategyConfigurationRequest();
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /* Interface PropertyChangeListener
    /* --------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ChangesetManagementPanel.SELECTED_CHANGESET_PROP)) {
            selectedChangeset = (Changeset) evt.getNewValue();
            updateSummary();
        } else if (evt.getPropertyName().equals(ChangesetManagementPanel.CLOSE_CHANGESET_AFTER_UPLOAD)) {
            closeChangesetAfterNextUpload = (Boolean) evt.getNewValue();
            updateSummary();
        } else if (evt.getPropertyName().equals(UploadedObjectsSummaryPanel.NUM_OBJECTS_TO_UPLOAD_PROP)) {
            numObjects = (Integer) evt.getNewValue();
            updateSummary();
        } else if (evt.getPropertyName().equals(UploadStrategySelectionPanel.UPLOAD_STRATEGY_SPECIFICATION_PROP)) {
            this.spec = (UploadStrategySpecification) evt.getNewValue();
            updateSummary();
        }
    }
}
