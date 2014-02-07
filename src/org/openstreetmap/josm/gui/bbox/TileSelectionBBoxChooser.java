// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * TileSelectionBBoxChooser allows to select a bounding box (i.e. for downloading) based
 * on OSM tile numbers.
 *
 * TileSelectionBBoxChooser can be embedded as component in a Swing container. Example:
 * <pre>
 *    JFrame f = new JFrame(....);
 *    f.getContentPane().setLayout(new BorderLayout()));
 *    TileSelectionBBoxChooser chooser = new TileSelectionBBoxChooser();
 *    f.add(chooser, BorderLayout.CENTER);
 *    chooser.addPropertyChangeListener(new PropertyChangeListener() {
 *        public void propertyChange(PropertyChangeEvent evt) {
 *            // listen for BBOX events
 *            if (evt.getPropertyName().equals(BBoxChooser.BBOX_PROP)) {
 *               Main.info("new bbox based on OSM tiles selected: " + (Bounds)evt.getNewValue());
 *            }
 *        }
 *    });
 *
 *    // init the chooser with a bounding box
 *    chooser.setBoundingBox(....);
 *
 *    f.setVisible(true);
 * </pre>
 */
public class TileSelectionBBoxChooser extends JPanel implements BBoxChooser{

    /** the current bounding box */
    private Bounds bbox;
    /** the map viewer showing the selected bounding box */
    private TileBoundsMapView mapViewer;
    /** a panel for entering a bounding box given by a  tile grid and a zoom level */
    private TileGridInputPanel pnlTileGrid;
    /** a panel for entering a bounding box given by the address of an individual OSM tile at
     *  a given zoom level
     */
    private TileAddressInputPanel pnlTileAddress;

    /**
     * builds the UI
     */
    protected void build() {
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(pnlTileGrid = new TileGridInputPanel(), gc);

        gc.gridx = 1;
        add(pnlTileAddress = new TileAddressInputPanel(), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(2,2,2,2);
        add(mapViewer = new TileBoundsMapView(), gc);
        mapViewer.setFocusable(false);
        mapViewer.setZoomContolsVisible(false);
        mapViewer.setMapMarkerVisible(false);

        pnlTileAddress.addPropertyChangeListener(pnlTileGrid);
        pnlTileGrid.addPropertyChangeListener(new TileBoundsChangeListener());
    }

    /**
     * Constructs a new {@code TileSelectionBBoxChooser}.
     */
    public TileSelectionBBoxChooser() {
        build();
    }

    /**
     * Replies the current bounding box. null, if no valid bounding box is currently selected.
     *
     */
    @Override
    public Bounds getBoundingBox() {
        return bbox;
    }

    /**
     * Sets the current bounding box.
     *
     * @param bbox the bounding box. null, if this widget isn't initialized with a bounding box
     */
    @Override
    public void setBoundingBox(Bounds bbox) {
        pnlTileGrid.initFromBoundingBox(bbox);
    }

    protected void refreshMapView() {
        if (bbox == null) return;

        // calc the screen coordinates for the new selection rectangle
        MapMarkerDot xmin_ymin = new MapMarkerDot(bbox.getMinLat(), bbox.getMinLon());
        MapMarkerDot xmax_ymax = new MapMarkerDot(bbox.getMaxLat(), bbox.getMaxLon());

        List<MapMarker> marker = new ArrayList<MapMarker>(2);
        marker.add(xmin_ymin);
        marker.add(xmax_ymax);
        mapViewer.setBoundingBox(bbox);
        mapViewer.setMapMarkerList(marker);
        mapViewer.setDisplayToFitMapMarkers();
        mapViewer.zoomOut();
    }

    /**
     * Computes the bounding box given a tile grid.
     *
     * @param tb the description of the tile grid
     * @return the bounding box
     */
    protected Bounds convertTileBoundsToBoundingBox(TileBounds tb) {
        LatLon min = getNorthWestLatLonOfTile(tb.min, tb.zoomLevel);
        Point p = new Point(tb.max);
        p.x++;
        p.y++;
        LatLon max = getNorthWestLatLonOfTile(p, tb.zoomLevel);
        return new Bounds(max.lat(), min.lon(), min.lat(), max.lon());
    }

    /**
     * Replies lat/lon of the north/west-corner of a tile at a specific zoom level
     *
     * @param tile  the tile address (x,y)
     * @param zoom the zoom level
     * @return lat/lon of the north/west-corner of a tile at a specific zoom level
     */
    protected LatLon getNorthWestLatLonOfTile(Point tile, int zoom) {
        double lon =  tile.x / Math.pow(2.0, zoom) * 360.0 - 180;
        double lat =  Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * tile.y) / Math.pow(2.0, zoom))));
        return new LatLon(lat, lon);
    }

    /**
     * Listens to changes in the selected tile bounds, refreshes the map view and emits
     * property change events for {@link BBoxChooser#BBOX_PROP}
     */
    class TileBoundsChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!evt.getPropertyName().equals(TileGridInputPanel.TILE_BOUNDS_PROP)) return;
            TileBounds tb = (TileBounds)evt.getNewValue();
            Bounds oldValue = TileSelectionBBoxChooser.this.bbox;
            TileSelectionBBoxChooser.this.bbox = convertTileBoundsToBoundingBox(tb);
            firePropertyChange(BBOX_PROP, oldValue, TileSelectionBBoxChooser.this.bbox);
            refreshMapView();
        }
    }

    /**
     * A panel for describing a rectangular area of OSM tiles at a given zoom level.
     *
     * The panel emits PropertyChangeEvents for the property {@link TileGridInputPanel#TILE_BOUNDS_PROP}
     * when the user successfully enters a valid tile grid specification.
     *
     */
    static private class TileGridInputPanel extends JPanel implements PropertyChangeListener{
        static public final String TILE_BOUNDS_PROP = TileGridInputPanel.class.getName() + ".tileBounds";

        private JosmTextField tfMaxY;
        private JosmTextField tfMinY;
        private JosmTextField tfMaxX;
        private JosmTextField tfMinX;
        private TileCoordinateValidator valMaxY;
        private TileCoordinateValidator valMinY;
        private TileCoordinateValidator valMaxX;
        private TileCoordinateValidator valMinX;
        private JSpinner spZoomLevel;
        private TileBoundsBuilder tileBoundsBuilder = new TileBoundsBuilder();
        private boolean doFireTileBoundChanged = true;

        protected JPanel buildTextPanel() {
            JPanel pnl = new JPanel(new BorderLayout());
            HtmlPanel msg = new HtmlPanel();
            msg.setText(tr("<html>Please select a <strong>range of OSM tiles</strong> at a given zoom level.</html>"));
            pnl.add(msg);
            return pnl;
        }

        protected JPanel buildZoomLevelPanel() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
            pnl.add(new JLabel(tr("Zoom level:")));
            pnl.add(spZoomLevel = new JSpinner(new SpinnerNumberModel(0,0,18,1)));
            spZoomLevel.addChangeListener(new ZomeLevelChangeHandler());
            spZoomLevel.addChangeListener(tileBoundsBuilder);
            return pnl;
        }

        protected JPanel buildTileGridInputPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            pnl.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = new Insets(0, 0, 2, 2);

            gc.gridwidth = 2;
            gc.gridx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            pnl.add(buildZoomLevelPanel(), gc);

            gc.gridwidth = 1;
            gc.gridy = 1;
            gc.gridx = 1;
            pnl.add(new JLabel(tr("from tile")), gc);

            gc.gridx = 2;
            pnl.add(new JLabel(tr("up to tile")), gc);

            gc.gridx = 0;
            gc.gridy = 2;
            gc.weightx = 0.0;
            pnl.add(new JLabel("X:"), gc);


            gc.gridx = 1;
            gc.weightx = 0.5;
            pnl.add(tfMinX = new JosmTextField(), gc);
            valMinX = new TileCoordinateValidator(tfMinX);
            SelectAllOnFocusGainedDecorator.decorate(tfMinX);
            tfMinX.addActionListener(tileBoundsBuilder);
            tfMinX.addFocusListener(tileBoundsBuilder);

            gc.gridx = 2;
            gc.weightx = 0.5;
            pnl.add(tfMaxX = new JosmTextField(), gc);
            valMaxX = new TileCoordinateValidator(tfMaxX);
            SelectAllOnFocusGainedDecorator.decorate(tfMaxX);
            tfMaxX.addActionListener(tileBoundsBuilder);
            tfMaxX.addFocusListener(tileBoundsBuilder);

            gc.gridx = 0;
            gc.gridy = 3;
            gc.weightx = 0.0;
            pnl.add(new JLabel("Y:"), gc);

            gc.gridx = 1;
            gc.weightx = 0.5;
            pnl.add(tfMinY = new JosmTextField(), gc);
            valMinY = new TileCoordinateValidator(tfMinY);
            SelectAllOnFocusGainedDecorator.decorate(tfMinY);
            tfMinY.addActionListener(tileBoundsBuilder);
            tfMinY.addFocusListener(tileBoundsBuilder);

            gc.gridx = 2;
            gc.weightx = 0.5;
            pnl.add(tfMaxY = new JosmTextField(), gc);
            valMaxY = new TileCoordinateValidator(tfMaxY);
            SelectAllOnFocusGainedDecorator.decorate(tfMaxY);
            tfMaxY.addActionListener(tileBoundsBuilder);
            tfMaxY.addFocusListener(tileBoundsBuilder);

            gc.gridy = 4;
            gc.gridx = 0;
            gc.gridwidth = 3;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            gc.fill = GridBagConstraints.BOTH;
            pnl.add(new JPanel(), gc);
            return pnl;
        }

        protected void build() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            add(buildTextPanel(), BorderLayout.NORTH);
            add(buildTileGridInputPanel(), BorderLayout.CENTER);

            Set<AWTKeyStroke> forwardKeys = new HashSet<AWTKeyStroke>(getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
            forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,forwardKeys);
        }

        public TileGridInputPanel() {
            build();
        }

        public void initFromBoundingBox(Bounds bbox) {
            if (bbox == null)
                return;
            TileBounds tb = new TileBounds();
            tb.zoomLevel = (Integer) spZoomLevel.getValue();
            tb.min = new Point(
                    Math.max(0,lonToTileX(tb.zoomLevel, bbox.getMinLon())),
                    Math.max(0,latToTileY(tb.zoomLevel, bbox.getMaxLat() - 0.00001))
            );
            tb.max = new Point(
                    Math.max(0,lonToTileX(tb.zoomLevel, bbox.getMaxLon())),
                    Math.max(0,latToTileY(tb.zoomLevel, bbox.getMinLat() - 0.00001))
            );
            doFireTileBoundChanged = false;
            setTileBounds(tb);
            doFireTileBoundChanged = true;
        }

        public static int latToTileY(int zoom, double lat) {
            if ((zoom < 3) || (zoom > 18)) return -1;
            double l = lat / 180 * Math.PI;
            double pf = Math.log(Math.tan(l) + (1/Math.cos(l)));
            return (int) ((1<<(zoom-1)) * (Math.PI - pf) / Math.PI);
        }

        public static int lonToTileX(int zoom, double lon) {
            if ((zoom < 3) || (zoom > 18)) return -1;
            return (int) ((1<<(zoom-3)) * (lon + 180.0) / 45.0);
        }

        public void setTileBounds(TileBounds tileBounds) {
            tfMinX.setText(Integer.toString(tileBounds.min.x));
            tfMinY.setText(Integer.toString(tileBounds.min.y));
            tfMaxX.setText(Integer.toString(tileBounds.max.x));
            tfMaxY.setText(Integer.toString(tileBounds.max.y));
            spZoomLevel.setValue(tileBounds.zoomLevel);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(TileAddressInputPanel.TILE_BOUNDS_PROP)) {
                TileBounds tb = (TileBounds)evt.getNewValue();
                setTileBounds(tb);
                fireTileBoundsChanged(tb);
            }
        }

        protected void fireTileBoundsChanged(TileBounds tb) {
            if (!doFireTileBoundChanged) return;
            firePropertyChange(TILE_BOUNDS_PROP, null, tb);
        }

        class ZomeLevelChangeHandler implements ChangeListener {
            @Override
            public void stateChanged(ChangeEvent e) {
                int zoomLevel = (Integer)spZoomLevel.getValue();
                valMaxX.setZoomLevel(zoomLevel);
                valMaxY.setZoomLevel(zoomLevel);
                valMinX.setZoomLevel(zoomLevel);
                valMinY.setZoomLevel(zoomLevel);
            }
        }

        class TileBoundsBuilder implements ActionListener, FocusListener, ChangeListener {
            protected void buildTileBounds() {
                if (!valMaxX.isValid()) return;
                if (!valMaxY.isValid()) return;
                if (!valMinX.isValid()) return;
                if (!valMinY.isValid()) return;
                Point min = new Point(valMinX.getTileIndex(), valMinY.getTileIndex());
                Point max = new Point(valMaxX.getTileIndex(), valMaxY.getTileIndex());
                int zoomlevel = (Integer)spZoomLevel.getValue();
                TileBounds tb = new TileBounds(min, max, zoomlevel);
                fireTileBoundsChanged(tb);
            }

            @Override
            public void focusGained(FocusEvent e) {/* irrelevant */}

            @Override
            public void focusLost(FocusEvent e) {
                buildTileBounds();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                buildTileBounds();
            }

            @Override
            public void stateChanged(ChangeEvent e) {
                buildTileBounds();
            }
        }
    }

    /**
     * A panel for entering the address of a single OSM tile at a given zoom level.
     *
     */
    static private class TileAddressInputPanel extends JPanel {

        static public final String TILE_BOUNDS_PROP = TileAddressInputPanel.class.getName() + ".tileBounds";

        private JosmTextField tfTileAddress;
        private TileAddressValidator valTileAddress;

        protected JPanel buildTextPanel() {
            JPanel pnl = new JPanel(new BorderLayout());
            HtmlPanel msg = new HtmlPanel();
            msg.setText(tr("<html>Alternatively you may enter a <strong>tile address</strong> for a single tile "
                    + "in the format <i>zoomlevel/x/y</i>, i.e. <i>15/256/223</i>. Tile addresses "
                    + "in the format <i>zoom,x,y</i> or <i>zoom;x;y</i> are valid too.</html>"));
            pnl.add(msg);
            return pnl;
        }

        protected JPanel buildTileAddressInputPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0,0,2,2);
            pnl.add(new JLabel(tr("Tile address:")), gc);

            gc.weightx = 1.0;
            gc.gridx = 1;
            pnl.add(tfTileAddress = new JosmTextField(), gc);
            valTileAddress = new TileAddressValidator(tfTileAddress);
            SelectAllOnFocusGainedDecorator.decorate(tfTileAddress);

            gc.weightx = 0.0;
            gc.gridx = 2;
            ApplyTileAddressAction applyTileAddressAction = new ApplyTileAddressAction();
            JButton btn = new JButton(applyTileAddressAction);
            btn.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
            pnl.add(btn, gc);
            tfTileAddress.addActionListener(applyTileAddressAction);
            return pnl;
        }

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.insets = new Insets(0,0,5,0);
            add(buildTextPanel(), gc);

            gc.gridy = 1;
            add(buildTileAddressInputPanel(), gc);

            // filler - grab remaining space
            gc.gridy = 2;
            gc.fill = GridBagConstraints.BOTH;
            gc.weighty = 1.0;
            add(new JPanel(), gc);
        }

        public TileAddressInputPanel() {
            setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            build();
        }

        protected void fireTileBoundsChanged(TileBounds tb){
            firePropertyChange(TILE_BOUNDS_PROP, null, tb);
        }

        class ApplyTileAddressAction extends AbstractAction {
            public ApplyTileAddressAction() {
                putValue(SMALL_ICON, ImageProvider.get("apply"));
                putValue(SHORT_DESCRIPTION, tr("Apply the tile address"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                TileBounds tb = valTileAddress.getTileBounds();
                if (tb != null) {
                    fireTileBoundsChanged(tb);
                }
            }
        }
    }

    /**
     * Validates a tile address
     */
    static private class TileAddressValidator extends AbstractTextComponentValidator {

        private TileBounds tileBounds = null;

        public TileAddressValidator(JTextComponent tc) throws IllegalArgumentException {
            super(tc);
        }

        @Override
        public boolean isValid() {
            String value = getComponent().getText().trim();
            Matcher m = Pattern.compile("(\\d+)[^\\d]+(\\d+)[^\\d]+(\\d+)").matcher(value);
            tileBounds = null;
            if (!m.matches()) return false;
            int zoom;
            try {
                zoom = Integer.parseInt(m.group(1));
            } catch(NumberFormatException e){
                return false;
            }
            if (zoom < 0 || zoom > 18) return false;

            int x;
            try {
                x = Integer.parseInt(m.group(2));
            } catch(NumberFormatException e){
                return false;
            }
            if (x < 0 || x >= Math.pow(2, zoom)) return false;
            int y;
            try {
                y = Integer.parseInt(m.group(3));
            } catch(NumberFormatException e){
                return false;
            }
            if (y < 0 || y >= Math.pow(2, zoom)) return false;

            tileBounds = new TileBounds(new Point(x,y), new Point(x,y), zoom);
            return true;
        }

        @Override
        public void validate() {
            if (isValid()) {
                feedbackValid(tr("Please enter a tile address"));
            } else {
                feedbackInvalid(tr("The current value isn''t a valid tile address", getComponent().getText()));
            }
        }

        public TileBounds getTileBounds() {
            return tileBounds;
        }
    }

    /**
     * Validates the x- or y-coordinate of a tile at a given zoom level.
     *
     */
    static private class TileCoordinateValidator extends AbstractTextComponentValidator {
        private int zoomLevel;
        private int tileIndex;

        public TileCoordinateValidator(JTextComponent tc) throws IllegalArgumentException {
            super(tc);
        }

        public void setZoomLevel(int zoomLevel) {
            this.zoomLevel = zoomLevel;
            validate();
        }

        @Override
        public boolean isValid() {
            String value = getComponent().getText().trim();
            try {
                if (value.isEmpty()) {
                    tileIndex = 0;
                } else {
                    tileIndex = Integer.parseInt(value);
                }
            } catch(NumberFormatException e) {
                return false;
            }
            if (tileIndex < 0 || tileIndex >= Math.pow(2, zoomLevel)) return false;

            return true;
        }

        @Override
        public void validate() {
            if (isValid()) {
                feedbackValid(tr("Please enter a tile index"));
            } else {
                feedbackInvalid(tr("The current value isn''t a valid tile index for the given zoom level", getComponent().getText()));
            }
        }

        public int getTileIndex() {
            return tileIndex;
        }
    }

    /**
     * Represents a rectangular area of tiles at a given zoom level.
     *
     */
    static private class TileBounds {
        public Point min;
        public Point max;
        public int zoomLevel;

        public TileBounds() {
            zoomLevel = 0;
            min = new Point(0,0);
            max = new Point(0,0);
        }

        public TileBounds(Point min, Point max, int zoomLevel) {
            this.min = min;
            this.max = max;
            this.zoomLevel = zoomLevel;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("min=").append(min.x).append(",").append(min.y).append(",");
            sb.append("max=").append(max.x).append(",").append(max.y).append(",");
            sb.append("zoom=").append(zoomLevel);
            return sb.toString();
        }
    }

    /**
     * The map view used in this bounding box chooser
     */
    static private class TileBoundsMapView extends JMapViewer {
        private Point min;
        private Point max;

        public TileBoundsMapView() {
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            TileLoader loader = tileController.getTileLoader();
            if (loader instanceof OsmTileLoader) {
                ((OsmTileLoader)loader).headers.put("User-Agent", Version.getInstance().getFullAgentString());
            }
        }

        public void setBoundingBox(Bounds bbox){
            if (bbox == null) {
                min = null;
                max = null;
            } else {
                int y1 = tileSource.LatToY(bbox.getMinLat(), MAX_ZOOM);
                int y2 = tileSource.LatToY(bbox.getMaxLat(), MAX_ZOOM);
                int x1 = tileSource.LonToX(bbox.getMinLon(), MAX_ZOOM);
                int x2 = tileSource.LonToX(bbox.getMaxLon(), MAX_ZOOM);

                min = new Point(Math.min(x1, x2), Math.min(y1, y2));
                max = new Point(Math.max(x1, x2), Math.max(y1, y2));
            }
            repaint();
        }

        protected Point getTopLeftCoordinates() {
            return new Point(center.x - (getWidth() / 2), center.y - (getHeight() / 2));
        }

        /**
         * Draw the map.
         */
        @Override
        public void paint(Graphics g) {
            try {
                super.paint(g);
                if (min == null || max == null) return;
                int zoomDiff = MAX_ZOOM - zoom;
                Point tlc = getTopLeftCoordinates();
                int x_min = (min.x >> zoomDiff) - tlc.x;
                int y_min = (min.y >> zoomDiff) - tlc.y;
                int x_max = (max.x >> zoomDiff) - tlc.x;
                int y_max = (max.y >> zoomDiff) - tlc.y;

                int w = x_max - x_min;
                int h = y_max - y_min;
                g.setColor(new Color(0.9f, 0.7f, 0.7f, 0.6f));
                g.fillRect(x_min, y_min, w, h);

                g.setColor(Color.BLACK);
                g.drawRect(x_min, y_min, w, h);
            } catch (Exception e) {
                Main.error(e);
            }
        }
    }
}
