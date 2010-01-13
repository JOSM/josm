// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.ImageProvider;

public class UploadParameterSummaryPanel extends JPanel implements HyperlinkListener, PropertyChangeListener{

    static private final Logger logger = Logger.getLogger(UploadParameterSummaryPanel.class.getName());

    private UploadStrategySpecification spec = new UploadStrategySpecification();
    private int numObjects;
    private JEditorPane jepMessage;
    private JLabel lblWarning;

    private Changeset selectedChangeset;
    private boolean closeChangesetAfterNextUpload;
    private ConfigurationParameterRequestHandler configHandler;

    protected String buildChangsetSummary() {
        StringBuffer msg = new StringBuffer();
        if (selectedChangeset == null || selectedChangeset.isNew()) {
            msg.append(tr("Objects are uploaded to a <strong>new changeset</strong>."));
        } else {
            String uploadComment = selectedChangeset.get("comment") == null ?
                    "" : selectedChangeset.get("comment");
            msg.append(tr("Objects are uploaded to the <strong>open changeset</strong> {0} with upload comment ''{1}''.",
                    selectedChangeset.getId(),
                    uploadComment
            ));
        }
        if (closeChangesetAfterNextUpload) {
            msg.append(tr("The changeset is going to be <strong>closed</strong> after this upload"));
        } else {
            msg.append(tr("The changeset is <strong>left open</strong> after this upload"));
        }
        msg.append(tr(" (<a href=\"urn:changeset-configuration\">" + tr("configure changeset") + "</a>)"));
        return msg.toString();
    }

    protected String buildStrategySummary() {
        if (spec == null)
            return "";
        // check whether we can use one changeset only or whether we have to use
        // multiple changesets
        //
        boolean useOneChangeset = true;
        int maxChunkSize = OsmApi.getOsmApi().getCapabilities().getMaxChangsetSize();
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
            } else if (numRequests > 1){
                msg = tr("Uploading <strong>{0} objects</strong> to <strong>1 changeset</strong> using <strong>{1} requests</strong>", numObjects, numRequests);
            }
            msg = msg + " (<a href=\"urn:advanced-configuration\">" + tr("advanced configuration") + "</a>)";
        } else {
            lblWarning.setVisible(true);
            if (numRequests == 0) {
                msg = tr("{0} objects exceed the max. allowed {1} objects in a changeset on the server ''{2}''. Please <a href=\"urn:advanced-configuration\">configure</a> how to proceed with <strong>multiple changesets</strong>",
                        numObjects, maxChunkSize, OsmApi.getOsmApi().getBaseUrl());
            } else if (numRequests > 1){
                msg = tr("Uploading <strong>{0} objects</strong> to <strong>multiple changesets</strong> using <strong>{1} requests</strong>", numObjects, numRequests);
                msg = msg + " (<a href=\"urn:advanced-configuration\">" + tr("advanced configuration") + "</a>)";
            }
        }
        return msg;
    }

    protected void build() {
        jepMessage = new JEditorPane("text/html", "");
        jepMessage.setOpaque(false);
        jepMessage.setEditable(false);
        jepMessage.addHyperlinkListener(this);
        Font f = UIManager.getFont("Label.font");
        StyleSheet ss = new StyleSheet();
        String rule = MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                f.isBold() ? "bold" : "normal",
                        f.isItalic() ? "italic" : "normal"
        );
        rule = "body {" + rule + "}";
        rule = MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                "bold",
                f.isItalic() ? "italic" : "normal"
        );
        rule = "strong {" + rule + "}";
        ss.addRule(rule);
        ss.addRule("a {text-decoration: underline; color: blue}");
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.setStyleSheet(ss);
        jepMessage.setEditorKit(kit);

        setLayout(new BorderLayout());
        add(jepMessage, BorderLayout.CENTER);
        lblWarning = new JLabel("");
        lblWarning.setVisible(false);
        lblWarning.setIcon(ImageProvider.get("warning-small.png"));
        lblWarning.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(lblWarning, BorderLayout.NORTH);
        add(pnl, BorderLayout.WEST);
    }

    public UploadParameterSummaryPanel() {
        build();
        updateSummary();
    }

    public void setConfigurationParameterRequestListener(ConfigurationParameterRequestHandler handler) {
        this.configHandler = handler;
    }

    public void setUploadStrategySpecification(UploadStrategySpecification spec) {
        this.spec = spec;
        updateSummary();
    }

    public void setNumObjects(int numObjects) {
        this.numObjects = numObjects;
        updateSummary();
    }

    public void setCloseChangesetAfterNextUpload(boolean value) {
        this.closeChangesetAfterNextUpload = value;
        updateSummary();
    }

    protected void updateSummary() {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>");
        sb.append(buildStrategySummary());
        sb.append("<br><br>");
        sb.append(buildChangsetSummary());
        sb.append("</html>");
        jepMessage.setText(sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* Interface HyperlinkListener
    /* --------------------------------------------------------------------- */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            if (e.getDescription() == null || configHandler == null)
                return;
            if (e.getDescription().equals("urn:changeset-configuration")) {
                configHandler.handleChangesetConfigurationRequest();
            } else if (e.getDescription().equals("urn:advanced-configuration")) {
                configHandler.handleUploadStrategyConfigurationRequest();
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /* Interface PropertyChangeListener
    /* --------------------------------------------------------------------- */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ChangesetManagementPanel.SELECTED_CHANGESET_PROP)) {
            selectedChangeset = (Changeset)evt.getNewValue();
            updateSummary();
        } else if (evt.getPropertyName().equals(ChangesetManagementPanel.CLOSE_CHANGESET_AFTER_UPLOAD)) {
            closeChangesetAfterNextUpload = (Boolean)evt.getNewValue();
            updateSummary();
        } else if (evt.getPropertyName().equals(UploadedObjectsSummaryPanel.NUM_OBJECTS_TO_UPLOAD_PROP)) {
            numObjects = (Integer)evt.getNewValue();
            updateSummary();
        } else if (evt.getPropertyName().equals(UploadStrategySelectionPanel.UPLOAD_STRATEGY_SPECIFICATION_PROP)) {
            this.spec = (UploadStrategySpecification)evt.getNewValue();
            updateSummary();
        }
    }
}
