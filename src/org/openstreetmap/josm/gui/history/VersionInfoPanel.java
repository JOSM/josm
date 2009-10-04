// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.UrlLabel;

/**
 * VersionInfoPanel is an UI component which displays the basic properties of a version
 * of a {@see OsmPrimitive}.
 * 
 */
public class VersionInfoPanel extends JPanel implements Observer{

    private PointInTimeType pointInTimeType;
    private HistoryBrowserModel model;
    private JLabel lblInfo;
    private UrlLabel lblUser;
    private UrlLabel lblChangeset;

    protected void build() {
        JPanel pnl1 = new JPanel();
        pnl1.setLayout(new FlowLayout(FlowLayout.LEFT));
        lblInfo = new JLabel();
        lblInfo.setHorizontalAlignment(JLabel.LEFT);
        pnl1.add(lblInfo);

        JPanel pnl2 = new JPanel();
        pnl2.setLayout(new FlowLayout(FlowLayout.LEFT));
        lblUser = new UrlLabel();
        pnl2.add(new JLabel(tr("User")));
        pnl2.add(lblUser);
        pnl2.add(new JLabel(tr("Changeset")));
        lblChangeset = new UrlLabel();
        pnl2.add(lblChangeset);

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        add(pnl1, gc);
        gc.gridy = 1;
        add(pnl2, gc);
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
        String text = tr(
                "<html>Version <strong>{0}</strong> created on <strong>{1}</strong>",
                Long.toString(primitive.getVersion()),
                new SimpleDateFormat().format(primitive.getTimestamp())
        );
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
        if (pointInTimeType == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "pointInTimeType"));
        if (model == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "model"));

        this.model = model;
        this.pointInTimeType = pointInTimeType;
        model.addObserver(this);
        build();
    }

    public void update(Observable o, Object arg) {
        lblInfo.setText(getInfoText());

        String url = AbstractInfoAction.getBaseBrowseUrl() + "/changeset/" + getPrimitive().getChangesetId();
        lblChangeset.setUrl(url);
        lblChangeset.setDescription(tr("{0}", getPrimitive().getChangesetId()));

        url = AbstractInfoAction.getBaseUserUrl() + "/" + getPrimitive().getUser();
        lblUser.setUrl(url);
        lblUser.setDescription(tr("{0}", getPrimitive().getUser()));
    }
}
