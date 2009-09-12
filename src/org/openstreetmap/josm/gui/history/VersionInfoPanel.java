// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * VersionInfoPanel is an UI component which displays the basic properties of a version
 * of a {@see OsmPrimitive}.
 * 
 */
public class VersionInfoPanel extends JPanel implements Observer{

    private PointInTimeType pointInTimeType;
    private HistoryBrowserModel model;
    private JLabel lblInfo;

    protected void build() {
        setLayout(new BorderLayout());
        lblInfo = new JLabel();
        lblInfo.setHorizontalAlignment(JLabel.LEFT);
        add(lblInfo, BorderLayout.CENTER);
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
        String url = AbstractInfoAction.getBaseBrowseUrl() + "/changeset/" + primitive.getChangesetId();
        String text = tr(
                "<html>Version <strong>{0}</strong> created on <strong>{1}</strong> by <strong>{2}</strong> in changeset <strong>{3}</strong></html>",
                Long.toString(primitive.getVersion()),
                new SimpleDateFormat().format(primitive.getTimestamp()),
                primitive.getUser(),
                primitive.getChangesetId()
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
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "pointInTimeType"));
        if (model == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "model"));

        this.model = model;
        this.pointInTimeType = pointInTimeType;
        model.addObserver(this);
        build();
    }

    public void update(Observable o, Object arg) {
        lblInfo.setText(getInfoText());
    }
}
