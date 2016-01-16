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

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Pair;

/**
 * An UI widget for displaying differences in the coordinates of two
 * {@link HistoryNode}s.
 * @since 2243
 */
public class CoordinateInfoViewer extends JPanel {

    /** the model */
    private transient HistoryBrowserModel model;
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
    /** the map panel showing the old+new coordinate */
    private MapViewer mapViewer;

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
        gc.insets = new Insets(5, 5, 5, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        referenceInfoPanel = new VersionInfoPanel(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
        add(referenceInfoPanel, gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        currentInfoPanel = new VersionInfoPanel(model, PointInTimeType.CURRENT_POINT_IN_TIME);
        add(currentInfoPanel, gc);

        // ---------------------------
        // the two coordinate panels
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(referenceLatLonViewer = new LatLonViewer(model, PointInTimeType.REFERENCE_POINT_IN_TIME), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
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

        // the map panel
        gc.gridx = 0;
        gc.gridy = 3;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.insets = new Insets(5, 5, 5, 5);
        add(mapViewer = new MapViewer(model), gc);
        mapViewer.setZoomContolsVisible(false);
    }

    /**
     * Constructs a new {@code CoordinateInfoViewer}.
     * @param model the model. Must not be null.
     * @throws IllegalArgumentException if model is null
     */
    public CoordinateInfoViewer(HistoryBrowserModel model) {
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
        if (mapViewer != null) {
            model.deleteObserver(mapViewer);
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
        if (mapViewer != null) {
            model.addObserver(mapViewer);
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
     * Pans the map to the old+new coordinate
     * @see JMapViewer#setDisplayToFitMapMarkers()
     */
    public void setDisplayToFitMapMarkers() {
        mapViewer.setDisplayToFitMapMarkers();
    }

    private static class Updater {
        private final transient HistoryBrowserModel model;
        private final PointInTimeType role;

        protected Updater(HistoryBrowserModel model, PointInTimeType role) {
            this.model = model;
            this.role = role;
        }

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

        protected final Pair<LatLon, LatLon> getCoordinates() {
            HistoryOsmPrimitive p = getPrimitive();
            HistoryOsmPrimitive opposite = getOppositePrimitive();
            if (!(p instanceof HistoryNode)) return null;
            if (!(opposite instanceof HistoryNode)) return null;
            HistoryNode node = (HistoryNode) p;
            HistoryNode oppositeNode = (HistoryNode) opposite;

            return Pair.create(node.getCoords(), oppositeNode.getCoords());
        }

    }

    /**
     * A UI widgets which displays the Lan/Lon-coordinates of a
     * {@link HistoryNode}.
     *
     */
    private static class LatLonViewer extends JPanel implements Observer {

        private JLabel lblLat;
        private JLabel lblLon;
        private final Updater updater;
        private final Color modifiedColor;

        protected void build() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            GridBagConstraints gc = new GridBagConstraints();

            // --------
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.NONE;
            gc.weightx = 0.0;
            gc.insets = new Insets(5, 5, 5, 5);
            gc.anchor = GridBagConstraints.NORTHWEST;
            add(new JLabel(tr("Latitude: ")), gc);

            // --------
            gc.gridx = 1;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(lblLat = new JLabel(), gc);
            GuiHelper.setBackgroundReadable(lblLat, Color.WHITE);
            lblLat.setOpaque(true);
            lblLat.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

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
            GuiHelper.setBackgroundReadable(lblLon, Color.WHITE);
            lblLon.setOpaque(true);
            lblLon.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        /**
         *
         * @param model a model
         * @param role the role for this viewer.
         */
        LatLonViewer(HistoryBrowserModel model, PointInTimeType role) {
            this.updater = new Updater(model, role);
            this.modifiedColor = PointInTimeType.CURRENT_POINT_IN_TIME.equals(role)
                    ? TwoColumnDiff.Item.DiffItemType.INSERTED.getColor()
                    : TwoColumnDiff.Item.DiffItemType.DELETED.getColor();
            build();
        }

        protected void refresh() {
            final Pair<LatLon, LatLon> coordinates = updater.getCoordinates();
            if (coordinates == null) return;
            final LatLon coord = coordinates.a;
            final LatLon oppositeCoord = coordinates.b;

            // display the coordinates
            lblLat.setText(coord != null ? coord.latToString(CoordinateFormat.DECIMAL_DEGREES) : tr("(none)"));
            lblLon.setText(coord != null ? coord.lonToString(CoordinateFormat.DECIMAL_DEGREES) : tr("(none)"));

            // update background color to reflect differences in the coordinates
            if (coord == oppositeCoord ||
                    (coord != null && oppositeCoord != null && coord.lat() == oppositeCoord.lat())) {
                GuiHelper.setBackgroundReadable(lblLat, Color.WHITE);
            } else {
                GuiHelper.setBackgroundReadable(lblLat, modifiedColor);
            }
            if (coord == oppositeCoord ||
                    (coord != null && oppositeCoord != null && coord.lon() == oppositeCoord.lon())) {
                GuiHelper.setBackgroundReadable(lblLon, Color.WHITE);
            } else {
                GuiHelper.setBackgroundReadable(lblLon, modifiedColor);
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            refresh();
        }
    }

    private static class MapViewer extends JMapViewer implements Observer {

        private final Updater updater;

        MapViewer(HistoryBrowserModel model) {
            this.updater = new Updater(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        }

        @Override
        public void update(Observable o, Object arg) {
            final Pair<LatLon, LatLon> coordinates = updater.getCoordinates();
            if (coordinates == null) {
                return;
            }

            removeAllMapMarkers();

            if (coordinates.a != null) {
                final MapMarkerDot oldMarker = new MapMarkerDot(coordinates.a.lat(), coordinates.a.lon());
                oldMarker.setBackColor(TwoColumnDiff.Item.DiffItemType.DELETED.getColor());
                addMapMarker(oldMarker);
            }
            if (coordinates.b != null) {
                final MapMarkerDot newMarker = new MapMarkerDot(coordinates.b.lat(), coordinates.b.lon());
                newMarker.setBackColor(TwoColumnDiff.Item.DiffItemType.INSERTED.getColor());
                addMapMarker(newMarker);
            }

            setDisplayToFitMapMarkers();
        }
    }

    private static class DistanceViewer extends JPanel implements Observer {

        private JLabel lblDistance;
        private final Updater updater;

        DistanceViewer(HistoryBrowserModel model) {
            this.updater = new Updater(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
            build();
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
            gc.insets = new Insets(5, 5, 5, 5);
            gc.anchor = GridBagConstraints.NORTHWEST;
            add(new JLabel(tr("Distance: ")), gc);

            // --------
            gc.gridx = 1;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(lblDistance = new JLabel(), gc);
            GuiHelper.setBackgroundReadable(lblDistance, Color.WHITE);
            lblDistance.setOpaque(true);
            lblDistance.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        protected void refresh() {
            final Pair<LatLon, LatLon> coordinates = updater.getCoordinates();
            if (coordinates == null) return;
            final LatLon coord = coordinates.a;
            final LatLon oppositeCoord = coordinates.b;

            // update distance
            //
            if (coord != null && oppositeCoord != null) {
                double distance = coord.greatCircleDistance(oppositeCoord);
                GuiHelper.setBackgroundReadable(lblDistance, distance > 0
                        ? TwoColumnDiff.Item.DiffItemType.CHANGED.getColor()
                        : Color.WHITE);
                lblDistance.setText(NavigatableComponent.getDistText(distance));
            } else {
                GuiHelper.setBackgroundReadable(lblDistance, coord != oppositeCoord
                        ? TwoColumnDiff.Item.DiffItemType.CHANGED.getColor()
                        : Color.WHITE);
                lblDistance.setText(tr("(none)"));
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            refresh();
        }
    }
}
