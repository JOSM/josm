// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.UrlLabel;

/**
 * VersionInfoPanel is an UI component which displays the basic properties of a version
 * of a {@see OsmPrimitive}.
 *
 */
public class VersionInfoPanel extends JPanel implements Observer{
    @SuppressWarnings("unused")
    static private final Logger logger = Logger.getLogger(VersionInfoPanel.class.getName());

    private PointInTimeType pointInTimeType;
    private HistoryBrowserModel model;
    private JMultilineLabel lblInfo;
    private UrlLabel lblUser;
    private UrlLabel lblChangeset;
    private JPanel pnlUserAndChangeset;

    protected void build() {
        JPanel pnl1 = new JPanel();
        pnl1.setLayout(new BorderLayout());
        lblInfo = new JMultilineLabel("");
        //lblInfo.setHorizontalAlignment(JLabel.LEFT);
        pnl1.add(lblInfo, BorderLayout.CENTER);

        pnlUserAndChangeset = new JPanel();
        pnlUserAndChangeset.setLayout(new FlowLayout(FlowLayout.LEFT));
        lblUser = new UrlLabel();
        pnlUserAndChangeset.add(new JLabel(tr("User")));
        pnlUserAndChangeset.add(lblUser);
        pnlUserAndChangeset.add(new JLabel(tr("Changeset")));
        lblChangeset = new UrlLabel();
        pnlUserAndChangeset.add(lblChangeset);

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
    }

    protected HistoryOsmPrimitive getPrimitive() {
        if (model == null || pointInTimeType == null)
            return null;
        return model.getPointInTime(pointInTimeType);
    }

    protected OsmDataLayer getEditLayer() {
        try {
            return Main.map.mapView.getEditLayer();
        } catch(NullPointerException e) {
            return null;
        }
    }

    protected String getInfoText() {
        HistoryOsmPrimitive primitive = getPrimitive();
        if (primitive == null)
            return "";
        String text;
        if (model.isLatest(primitive)) {
            text = tr("<html>Version <strong>{0}</strong> currently edited in layer ''{1}''</html>",
                    Long.toString(primitive.getVersion()),
                    getEditLayer() == null ? tr("unknown") : getEditLayer().getName()
            );
        } else {
            text = tr(
                    "<html>Version <strong>{0}</strong> created on <strong>{1}</strong></html>",
                    Long.toString(primitive.getVersion()),
                    new SimpleDateFormat().format(primitive.getTimestamp())
            );
        }
        return text;
    }

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

    public void update(Observable o, Object arg) {
        lblInfo.setText(getInfoText());

        if (!model.isLatest(getPrimitive())) {
            String url = AbstractInfoAction.getBaseBrowseUrl() + "/changeset/" + getPrimitive().getChangesetId();
            lblChangeset.setUrl(url);
            lblChangeset.setDescription(Long.toString(getPrimitive().getChangesetId()));

            try {
                if (getPrimitive().getUid() != -1) {
                    url = AbstractInfoAction.getBaseUserUrl() + "/" +  URLEncoder.encode(getPrimitive().getUser(), "UTF-8").replaceAll("\\+", "%20");
                    lblUser.setUrl(url);
                } else {
                    lblUser.setUrl(null);
                }
            } catch(UnsupportedEncodingException e) {
                e.printStackTrace();
                lblUser.setUrl(null);
            }
            String username = getPrimitive().getUser();
            lblUser.setDescription(username);
        } else {
            String user = Main.pref.get("osm-server.username");
            if (user == null) {
                lblUser.setDescription(tr("anonymous"));
            } else {
                try {
                    String url = AbstractInfoAction.getBaseUserUrl() + "/" +  URLEncoder.encode(user, "UTF-8").replaceAll("\\+", "%20");
                    lblUser.setUrl(url);
                } catch(UnsupportedEncodingException e) {
                    e.printStackTrace();
                    lblUser.setUrl(null);
                }
                lblUser.setDescription(user);
            }
            lblChangeset.setDescription(tr("none"));
            lblChangeset.setUrl(null);
        }
    }
}
