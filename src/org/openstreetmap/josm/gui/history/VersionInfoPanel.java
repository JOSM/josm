// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * VersionInfoPanel is an UI component which displays the basic properties of a version
 * of a {@link OsmPrimitive}.
 *
 */
public class VersionInfoPanel extends JPanel implements Observer{
    private PointInTimeType pointInTimeType;
    private HistoryBrowserModel model;
    private JMultilineLabel lblInfo;
    private UrlLabel lblUser;
    private UrlLabel lblChangeset;
    private JPanel pnlChangesetComment;
    private JPanel pnlChangesetSource;
    private JLabel lblComment;
    private JLabel lblSource;
    private JTextArea lblChangesetComment;
    private JTextArea lblChangesetSource;

    protected static JTextArea buildTextArea(String tooltip) {
        JTextArea lbl = new JTextArea();
        lbl.setLineWrap(true);
        lbl.setWrapStyleWord(true);
        lbl.setEditable(false);
        lbl.setOpaque(false);
        lbl.setToolTipText(tooltip);
        return lbl;
    }

    protected static JLabel buildLabel(String text, String tooltip, JTextArea textArea) {
        // We need text field to be a JTextArea for line wrapping but cannot put HTML code in here,
        // so create a separate JLabel with same characteristics (margin, font)
        JLabel lbl = new JLabel("<html><p style='margin-top:"+textArea.getMargin().top+"'>"+text+"</html>");
        lbl.setFont(textArea.getFont());
        lbl.setToolTipText(tooltip);
        return lbl;
    }

    protected static JPanel buildTextPanel(JLabel label, JTextArea textArea) {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(label, GBC.std().anchor(GBC.NORTHWEST));
        pnl.add(textArea, GBC.eol().fill());
        return pnl;
    }

    protected void build() {
        JPanel pnl1 = new JPanel(new BorderLayout());
        lblInfo = new JMultilineLabel("");
        pnl1.add(lblInfo, BorderLayout.CENTER);

        JPanel pnlUserAndChangeset = new JPanel(new GridLayout(2,2));
        lblUser = new UrlLabel("", 2);
        pnlUserAndChangeset.add(new JLabel(tr("User:")));
        pnlUserAndChangeset.add(lblUser);
        pnlUserAndChangeset.add(new JLabel(tr("Changeset:")));
        lblChangeset = new UrlLabel("", 2);
        pnlUserAndChangeset.add(lblChangeset);

        lblChangesetComment = buildTextArea(tr("Changeset comment"));
        lblChangesetSource = buildTextArea(tr("Changeset source"));
        
        lblComment = buildLabel(/*I18n: comment*/tr("<b>c</b>:"), tr("comment"), lblChangesetComment);
        lblSource = buildLabel(/*I18n: source*/tr("<b>s</b>:"), tr("source"), lblChangesetSource);
        
        pnlChangesetComment = buildTextPanel(lblComment, lblChangesetComment);
        pnlChangesetSource = buildTextPanel(lblSource, lblChangesetSource);

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(pnl1, gc);
        gc.gridy = 1;
        gc.weighty = 0.0;
        add(pnlUserAndChangeset, gc);
        gc.gridy = 2;
        add(pnlChangesetComment, gc);
        gc.gridy = 3;
        add(pnlChangesetSource, gc);
    }

    protected HistoryOsmPrimitive getPrimitive() {
        if (model == null || pointInTimeType == null)
            return null;
        return model.getPointInTime(pointInTimeType);
    }

    protected String getInfoText() {
        HistoryOsmPrimitive primitive = getPrimitive();
        if (primitive == null)
            return "";
        String text;
        if (model.isLatest(primitive)) {
            OsmDataLayer editLayer = Main.main.getEditLayer();
            text = tr("<html>Version <strong>{0}</strong> currently edited in layer ''{1}''</html>",
                    Long.toString(primitive.getVersion()),
                    editLayer == null ? tr("unknown") : editLayer.getName()
                    );
        } else {
            String date = "?";
            if (primitive.getTimestamp() != null) {
                date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(primitive.getTimestamp());
            }
            text = tr(
                    "<html>Version <strong>{0}</strong> created on <strong>{1}</strong></html>",
                    Long.toString(primitive.getVersion()), date);
        }
        return text;
    }

    /**
     * Constructs a new {@code VersionInfoPanel}.
     */
    public VersionInfoPanel() {
        pointInTimeType = null;
        model = null;
        build();
    }

    /**
     * constructor
     *
     * @param model  the model (must not be null)
     * @param pointInTimeType the point in time this panel visualizes (must not be null)
     * @exception IllegalArgumentException thrown, if model is null
     * @exception IllegalArgumentException thrown, if pointInTimeType is null
     *
     */
    public VersionInfoPanel(HistoryBrowserModel model, PointInTimeType pointInTimeType) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        CheckParameterUtil.ensureParameterNotNull(model, "model");

        this.model = model;
        this.pointInTimeType = pointInTimeType;
        model.addObserver(this);
        build();
    }

    protected static String getUserUrl(String username) throws UnsupportedEncodingException {
        return AbstractInfoAction.getBaseUserUrl() + "/" +  URLEncoder.encode(username, "UTF-8").replaceAll("\\+", "%20");
    }

    @Override
    public void update(Observable o, Object arg) {
        lblInfo.setText(getInfoText());

        HistoryOsmPrimitive primitive = getPrimitive();
        Changeset cs = primitive.getChangeset();

        if (!model.isLatest(primitive)) {
            User user = primitive.getUser();
            String url = AbstractInfoAction.getBaseBrowseUrl() + "/changeset/" + primitive.getChangesetId();
            lblChangeset.setUrl(url);
            lblChangeset.setDescription(Long.toString(primitive.getChangesetId()));

            String username = "";
            if (user != null) {
                username = user.getName();
            }
            lblUser.setDescription(username);
            try {
                if (user != null && user != User.getAnonymous()) {
                    lblUser.setUrl(getUserUrl(username));
                } else {
                    lblUser.setUrl(null);
                }
            } catch(UnsupportedEncodingException e) {
                Main.error(e);
                lblUser.setUrl(null);
            }
        } else {
            String username = JosmUserIdentityManager.getInstance().getUserName();
            if (username == null) {
                lblUser.setDescription(tr("anonymous"));
                lblUser.setUrl(null);
            } else {
                lblUser.setDescription(username);
                try {
                    lblUser.setUrl(getUserUrl(username));
                } catch(UnsupportedEncodingException e) {
                    Main.error(e);
                    lblUser.setUrl(null);
                }
            }
            lblChangeset.setDescription(tr("none"));
            lblChangeset.setUrl(null);
        }

        final Changeset oppCs = model.getPointInTime(pointInTimeType.opposite()).getChangeset();
        updateText(cs, "comment", lblChangesetComment, lblComment, oppCs, pnlChangesetComment);
        updateText(cs, "source", lblChangesetSource, lblSource, oppCs, pnlChangesetSource);
    }
    
    protected static void updateText(Changeset cs, String attr, JTextArea textArea, JLabel label, Changeset oppCs, JPanel panel) {
        final String text = cs != null ? cs.get(attr) : null;
        // Update text, hide prefixing label if empty
        label.setVisible(text != null && !Utils.strip(text).isEmpty());
        textArea.setText(text);
        // Hide panel if values of both versions are empty
        panel.setVisible(text != null || (oppCs != null && oppCs.get(attr) != null));
    }
}
