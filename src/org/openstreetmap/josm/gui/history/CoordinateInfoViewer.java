// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.bbox.JosmMapViewer;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Pair;

/**
 * An UI widget for displaying differences in the coordinates of two
 * {@link HistoryNode}s.
 * @since 2243
 */
public class CoordinateInfoViewer extends HistoryBrowserPanel {

    /** the info panel for coordinates for the node in role REFERENCE_POINT_IN_TIME */
    private LatLonViewer referenceLatLonViewer;
    /** the info panel for coordinates for the node in role CURRENT_POINT_IN_TIME */
    private LatLonViewer currentLatLonViewer;
    /** the info panel for distance between the two coordinates */
    private DistanceViewer distanceViewer;
    /** the map panel showing the old+new coordinate */
    private MapViewer mapViewer;

    protected void build() {
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
        referenceLatLonViewer = new LatLonViewer(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
        add(referenceLatLonViewer, gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        currentLatLonViewer = new LatLonViewer(model, PointInTimeType.CURRENT_POINT_IN_TIME);
        add(currentLatLonViewer, gc);

        // --------------------
        // the distance panel
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        distanceViewer = new DistanceViewer(model);
        add(distanceViewer, gc);

        // the map panel
        gc.gridx = 0;
        gc.gridy = 3;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        mapViewer = new MapViewer(model);
        add(mapViewer, gc);
        mapViewer.setZoomControlsVisible(false);
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
        registerAsChangeListener(model);
    }

    @Override
    protected void unregisterAsChangeListener(HistoryBrowserModel model) {
        super.unregisterAsChangeListener(model);
        if (currentLatLonViewer != null) {
            model.removeChangeListener(currentLatLonViewer);
        }
        if (referenceLatLonViewer != null) {
            model.removeChangeListener(referenceLatLonViewer);
        }
        if (distanceViewer != null) {
            model.removeChangeListener(distanceViewer);
        }
        if (mapViewer != null) {
            model.removeChangeListener(mapViewer);
        }
    }

    @Override
    protected void registerAsChangeListener(HistoryBrowserModel model) {
        super.registerAsChangeListener(model);
        if (currentLatLonViewer != null) {
            model.addChangeListener(currentLatLonViewer);
        }
        if (referenceLatLonViewer != null) {
            model.addChangeListener(referenceLatLonViewer);
        }
        if (distanceViewer != null) {
            model.addChangeListener(distanceViewer);
        }
        if (mapViewer != null) {
            model.addChangeListener(mapViewer);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        referenceLatLonViewer.destroy();
        currentLatLonViewer.destroy();
        distanceViewer.destroy();
    }

    /**
     * Pans the map to the old+new coordinate
     * @see JMapViewer#setDisplayToFitMapMarkers()
     */
    public void setDisplayToFitMapMarkers() {
        mapViewer.setDisplayToFitMapMarkers();
    }

    private static JosmTextArea newTextArea() {
        JosmTextArea area = new JosmTextArea();
        GuiHelper.setBackgroundReadable(area, Color.WHITE);
        area.setEditable(false);
        area.setOpaque(true);
        area.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        area.setFont(UIManager.getFont("Label.font"));
        return area;
    }

    private static class Updater {
        private final HistoryBrowserModel model;
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
            if (!(p instanceof HistoryNode)) return null;
            HistoryOsmPrimitive opposite = getOppositePrimitive();
            if (!(opposite instanceof HistoryNode)) return null;
            HistoryNode node = (HistoryNode) p;
            HistoryNode oppositeNode = (HistoryNode) opposite;

            return Pair.create(node.getCoords(), oppositeNode.getCoords());
        }
    }

    /**
     * A UI widgets which displays the Lan/Lon-coordinates of a {@link HistoryNode}.
     */
    private static class LatLonViewer extends JPanel implements ChangeListener, Destroyable {

        private final JosmTextArea lblLat = newTextArea();
        private final JosmTextArea lblLon = newTextArea();
        private final transient Updater updater;
        private final Color modifiedColor;

        protected void build() {
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
            add(lblLat, gc);

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
            add(lblLon, gc);
        }

        /**
         * Constructs a new {@code LatLonViewer}.
         * @param model a model
         * @param role the role for this viewer.
         */
        LatLonViewer(HistoryBrowserModel model, PointInTimeType role) {
            super(new GridBagLayout());
            this.updater = new Updater(model, role);
            this.modifiedColor = PointInTimeType.CURRENT_POINT_IN_TIME == role
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
            lblLat.setText(coord != null ? DecimalDegreesCoordinateFormat.INSTANCE.latToString(coord) : tr("(none)"));
            lblLon.setText(coord != null ? DecimalDegreesCoordinateFormat.INSTANCE.lonToString(coord) : tr("(none)"));

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
        public void stateChanged(ChangeEvent e) {
            refresh();
        }

        @Override
        public void destroy() {
            lblLat.destroy();
            lblLon.destroy();
        }
    }

    private static class MapViewer extends JosmMapViewer implements ChangeListener {

        private final transient Updater updater;

        MapViewer(HistoryBrowserModel model) {
            this.updater = new Updater(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
            setTileSource(SlippyMapBBoxChooser.DefaultOsmTileSourceProvider.get()); // for attribution
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        getAttribution().handleAttribution(e.getPoint(), true);
                    }
                }
            });
        }

        @Override
        public void stateChanged(ChangeEvent e) {
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

            super.setDisplayToFitMapMarkers();
        }
    }

    private static class DistanceViewer extends JPanel implements ChangeListener, Destroyable {

        private final JosmTextArea lblDistance = newTextArea();
        private final transient Updater updater;

        DistanceViewer(HistoryBrowserModel model) {
            super(new GridBagLayout());
            updater = new Updater(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
            build();
        }

        protected void build() {
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
            add(lblDistance, gc);
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
        public void stateChanged(ChangeEvent e) {
            refresh();
        }

        @Override
        public void destroy() {
            lblDistance.destroy();
        }
    }
}
