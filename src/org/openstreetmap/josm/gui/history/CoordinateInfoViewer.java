// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * An UI widget for displaying differences in the coordinates of two
 * {@link HistoryNode}s.
 *
 */
public class CoordinateInfoViewer extends JPanel {

    /** background color used when the coordinates are different */
    public final static Color BGCOLOR_DIFFERENCE = new Color(255,197,197);

    /** the model */
    private HistoryBrowserModel model;
    /** the common info panel for the history node in role REFERENCE_POINT_IN_TIME */
    private VersionInfoPanel referenceInfoPanel;
    /** the common info panel for the history node in role CURRENT_POINT_IN_TIME */
    private VersionInfoPanel currentInfoPanel;
    /** the info panel for coordinates for the node in role REFERENCE_POINT_IN_TIME */
    private LatLonViewer referenceLatLonViewer;
    /** the info panel for coordinates for the node in role CURRENT_POINT_IN_TIME */
    private LatLonViewer currentLatLonViewer;
    /** the info panel for distance between the two coordinates */
    private DistanceViewer distanceViewer;

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // ---------------------------
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.insets = new Insets(5,5,5,0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        referenceInfoPanel = new VersionInfoPanel(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
        add(referenceInfoPanel,gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        currentInfoPanel = new VersionInfoPanel(model, PointInTimeType.CURRENT_POINT_IN_TIME);
        add(currentInfoPanel,gc);

        // ---------------------------
        // the two coordinate panels
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(referenceLatLonViewer = new LatLonViewer(model, PointInTimeType.REFERENCE_POINT_IN_TIME), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(currentLatLonViewer = new LatLonViewer(model, PointInTimeType.CURRENT_POINT_IN_TIME), gc);

        // --------------------
        // the distance panel
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        add(distanceViewer = new DistanceViewer(model), gc);
    }

    /**
     *
     * @param model the model. Must not be null.
     * @throws IllegalArgumentException thrown if model is null
     */
    public CoordinateInfoViewer(HistoryBrowserModel model) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(model, "model");
        setModel(model);
        build();
        registerAsObserver(model);
    }

    protected void unregisterAsObserver(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.deleteObserver(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.deleteObserver(referenceInfoPanel);
        }
        if (currentLatLonViewer != null) {
            model.deleteObserver(currentLatLonViewer);
        }
        if (referenceLatLonViewer != null) {
            model.deleteObserver(referenceLatLonViewer);
        }
        if (distanceViewer != null) {
            model.deleteObserver(distanceViewer);
        }
    }

    protected void registerAsObserver(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.addObserver(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.addObserver(referenceInfoPanel);
        }
        if (currentLatLonViewer != null) {
            model.addObserver(currentLatLonViewer);
        }
        if (referenceLatLonViewer != null) {
            model.addObserver(referenceLatLonViewer);
        }
        if (distanceViewer != null) {
            model.addObserver(distanceViewer);
        }
    }

    /**
     * Sets the model for this viewer
     *
     * @param model the model.
     */
    public void setModel(HistoryBrowserModel model) {
        if (this.model != null) {
            unregisterAsObserver(model);
        }
        this.model = model;
        if (this.model != null) {
            registerAsObserver(model);
        }
    }

    /**
     * A UI widgets which displays the Lan/Lon-coordinates of a
     * {@link HistoryNode}.
     *
     */
    private static class LatLonViewer extends JPanel implements Observer{

        private JLabel lblLat;
        private JLabel lblLon;
        private HistoryBrowserModel model;
        private PointInTimeType role;

        protected HistoryOsmPrimitive getPrimitive() {
            if (model == null || role == null)
                return null;
            return model.getPointInTime(role);
        }

        protected HistoryOsmPrimitive getOppositePrimitive() {
            if (model == null || role == null)
                return null;
            return model.getPointInTime(role.opposite());
        }

        protected void build() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            GridBagConstraints gc = new GridBagConstraints();

            // --------
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.NONE;
            gc.weightx = 0.0;
            gc.insets = new Insets(5,5,5,5);
            gc.anchor = GridBagConstraints.NORTHWEST;
            add(new JLabel(tr("Latitude: ")), gc);

            // --------
            gc.gridx = 1;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(lblLat = new JLabel(), gc);
            lblLat.setBackground(Color.WHITE);
            lblLat.setOpaque(true);
            lblLat.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

            // --------
            gc.gridx = 0;
            gc.gridy = 1;
            gc.fill = GridBagConstraints.NONE;
            gc.weightx = 0.0;
            gc.anchor = GridBagConstraints.NORTHWEST;
            add(new JLabel(tr("Longitude: ")), gc);

            // --------
            gc.gridx = 1;
            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(lblLon = new JLabel(), gc);
            lblLon.setBackground(Color.WHITE);
            lblLon.setOpaque(true);
            lblLon.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

            // fill the remaining space
            gc.gridx = 0;
            gc.gridy = 2;
            gc.gridwidth = 2;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(new JPanel(), gc);
        }

        /**
         *
         * @param model a model
         * @param role the role for this viewer.
         */
        public LatLonViewer(HistoryBrowserModel model, PointInTimeType role) {
            build();
            this.model = model;
            this.role = role;
        }

        protected void refresh() {
            HistoryOsmPrimitive p = getPrimitive();
            HistoryOsmPrimitive  opposite = getOppositePrimitive();
            if (!(p instanceof HistoryNode)) return;
            if (!(opposite instanceof HistoryNode)) return;
            HistoryNode node = (HistoryNode)p;
            HistoryNode oppositeNode = (HistoryNode) opposite;

            LatLon coord = node.getCoords();
            LatLon oppositeCoord = oppositeNode.getCoords();

            // display the coordinates
            //
            lblLat.setText(coord != null ? coord.latToString(CoordinateFormat.DECIMAL_DEGREES) : tr("(none)"));
            lblLon.setText(coord != null ? coord.lonToString(CoordinateFormat.DECIMAL_DEGREES) : tr("(none)"));

            // update background color to reflect differences in the coordinates
            //
            if (coord == oppositeCoord ||
                    (coord != null && oppositeCoord != null && coord.lat() == oppositeCoord.lat())) {
                lblLat.setBackground(Color.WHITE);
            } else {
                lblLat.setBackground(BGCOLOR_DIFFERENCE);
            }
            if (coord == oppositeCoord ||
                    (coord != null && oppositeCoord != null && coord.lon() == oppositeCoord.lon())) {
                lblLon.setBackground(Color.WHITE);
            } else {
                lblLon.setBackground(BGCOLOR_DIFFERENCE);
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            refresh();
        }
    }

    private static class DistanceViewer extends LatLonViewer {

        private JLabel lblDistance;

        public DistanceViewer(HistoryBrowserModel model) {
            super(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
        }

        @Override
        protected void build() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            GridBagConstraints gc = new GridBagConstraints();

            // --------
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.NONE;
            gc.weightx = 0.0;
            gc.insets = new Insets(5,5,5,5);
            gc.anchor = GridBagConstraints.NORTHWEST;
            add(new JLabel(tr("Distance: ")), gc);

            // --------
            gc.gridx = 1;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(lblDistance = new JLabel(), gc);
            lblDistance.setBackground(Color.WHITE);
            lblDistance.setOpaque(true);
            lblDistance.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        }

        @Override
        protected void refresh() {
            HistoryOsmPrimitive p = getPrimitive();
            HistoryOsmPrimitive opposite = getOppositePrimitive();
            if (!(p instanceof HistoryNode)) return;
            if (!(opposite instanceof HistoryNode)) return;
            HistoryNode node = (HistoryNode) p;
            HistoryNode oppositeNode = (HistoryNode) opposite;

            LatLon coord = node.getCoords();
            LatLon oppositeCoord = oppositeNode.getCoords();

            // update distance
            //
            if (coord != null && oppositeCoord != null) {
                double distance = coord.greatCircleDistance(oppositeCoord);
                if (distance > 0) {
                    lblDistance.setBackground(BGCOLOR_DIFFERENCE);
                } else {
                    lblDistance.setBackground(Color.WHITE);
                }
                lblDistance.setText(NavigatableComponent.getDistText(distance));
            } else {
                lblDistance.setBackground(coord != oppositeCoord ? BGCOLOR_DIFFERENCE : Color.WHITE);
                lblDistance.setText(tr("(none)"));
            }
        }
    }
}
