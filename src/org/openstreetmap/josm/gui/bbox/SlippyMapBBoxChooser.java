// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.ButtonModel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractCachedTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * This panel displays a map and lets the user chose a {@link BBox}.
 */
public class SlippyMapBBoxChooser extends JMapViewer implements BBoxChooser, ChangeListener,
    MainLayerManager.ActiveLayerChangeListener, MainLayerManager.LayerChangeListener {
    /**
     * A list of tile sources that can be used for displaying the map.
     */
    @FunctionalInterface
    public interface TileSourceProvider {
        /**
         * Gets the tile sources that can be displayed
         * @return The tile sources
         */
        List<TileSource> getTileSources();
    }

    /**
     * TileSource provider for the slippymap chooser.
     * @since 14300
     */
    public abstract static class AbstractImageryInfoBasedTileSourceProvider implements TileSourceProvider {
        /**
         * Returns the list of imagery infos backing tile sources.
         * @return the list of imagery infos backing tile sources
         */
        public abstract List<ImageryInfo> getImageryInfos();

        @Override
        public List<TileSource> getTileSources() {
            if (!TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get()) return Collections.<TileSource>emptyList();
            List<TileSource> sources = new ArrayList<>();
            for (ImageryInfo info : this.getImageryInfos()) {
                try {
                    TileSource source = TMSLayer.getTileSourceStatic(info);
                    if (source != null) {
                        sources.add(source);
                    }
                } catch (IllegalArgumentException ex) {
                    Logging.warn(ex);
                    if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                        JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                                ex.getMessage(), tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
            return sources;
        }
    }

    /**
     * TileSource provider for the slippymap chooser - providing sources from imagery sources menu
     * @since 14300
     */
    public static class TMSTileSourceProvider extends AbstractImageryInfoBasedTileSourceProvider {
        @Override
        public List<ImageryInfo> getImageryInfos() {
            return ImageryLayerInfo.instance.getLayers();
        }
    }

    /**
     * TileSource provider for the slippymap chooser - providing sources from current layers
     * @since 14300
     */
    public static class CurrentLayersTileSourceProvider extends AbstractImageryInfoBasedTileSourceProvider {
        @Override
        public List<ImageryInfo> getImageryInfos() {
            return MainApplication.getLayerManager().getLayers().stream().filter(
                layer -> layer instanceof ImageryLayer
            ).map(
                layer -> ((ImageryLayer) layer).getInfo()
            ).collect(Collectors.toList());
        }
    }

    /**
     * Plugins that wish to add custom tile sources to slippy map choose should call this method
     * @param tileSourceProvider new tile source provider
     */
    public static void addTileSourceProvider(TileSourceProvider tileSourceProvider) {
        providers.addIfAbsent(tileSourceProvider);
    }

    private static CopyOnWriteArrayList<TileSourceProvider> providers = new CopyOnWriteArrayList<>();
    static {
        addTileSourceProvider(() -> Arrays.<TileSource>asList(new OsmTileSource.Mapnik()));
        addTileSourceProvider(new TMSTileSourceProvider());
        addTileSourceProvider(new CurrentLayersTileSourceProvider());
    }

    private static final StringProperty PROP_MAPSTYLE = new StringProperty("slippy_map_chooser.mapstyle", "Mapnik");
    private static final BooleanProperty PROP_SHOWDLAREA = new BooleanProperty("slippy_map_chooser.show_downloaded_area", true);
    /**
     * The property name used for the resize button.
     * @see #addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public static final String RESIZE_PROP = SlippyMapBBoxChooser.class.getName() + ".resize";

    private final transient TileLoader cachedLoader;
    private final transient OsmTileLoader uncachedLoader;

    private final SizeButton iSizeButton;
    private final ButtonModel showDownloadAreaButtonModel;
    private final SourceButton iSourceButton;
    private transient Bounds bbox;

    // upper left and lower right corners of the selection rectangle (x/y on ZOOM_MAX)
    private transient ICoordinate iSelectionRectStart;
    private transient ICoordinate iSelectionRectEnd;

    /**
     * Constructs a new {@code SlippyMapBBoxChooser}.
     */
    public SlippyMapBBoxChooser() {
        debug = Logging.isDebugEnabled();
        SpringLayout springLayout = new SpringLayout();
        setLayout(springLayout);

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Version.getInstance().getFullAgentString());

        TileLoaderFactory cachedLoaderFactory = AbstractCachedTileSourceLayer.getTileLoaderFactory("TMS", TMSCachedTileLoader.class);
        if (cachedLoaderFactory != null) {
            cachedLoader = cachedLoaderFactory.makeTileLoader(this, headers, TimeUnit.HOURS.toSeconds(1));
        } else {
            cachedLoader = null;
        }

        uncachedLoader = new OsmTileLoader(this);
        uncachedLoader.headers.putAll(headers);
        setZoomControlsVisible(Config.getPref().getBoolean("slippy_map_chooser.zoomcontrols", false));
        setMapMarkerVisible(false);
        setMinimumSize(new Dimension(350, 350 / 2));
        // We need to set an initial size - this prevents a wrong zoom selection
        // for the area before the component has been displayed the first time
        setBounds(new Rectangle(getMinimumSize()));
        if (cachedLoader == null) {
            setFileCacheEnabled(false);
        } else {
            setFileCacheEnabled(Config.getPref().getBoolean("slippy_map_chooser.file_cache", true));
        }
        setMaxTilesInMemory(Config.getPref().getInt("slippy_map_chooser.max_tiles", 1000));

        List<TileSource> tileSources = new ArrayList<>(getAllTileSources().values());

        this.showDownloadAreaButtonModel = new JToggleButton.ToggleButtonModel();
        this.showDownloadAreaButtonModel.setSelected(PROP_SHOWDLAREA.get());
        this.showDownloadAreaButtonModel.addChangeListener(this);
        iSourceButton = new SourceButton(this, tileSources, this.showDownloadAreaButtonModel);
        add(iSourceButton);
        springLayout.putConstraint(SpringLayout.EAST, iSourceButton, -2, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.NORTH, iSourceButton, 2, SpringLayout.NORTH, this);

        iSizeButton = new SizeButton(this);
        add(iSizeButton);

        String mapStyle = PROP_MAPSTYLE.get();
        boolean foundSource = false;
        for (TileSource source: tileSources) {
            if (source.getName().equals(mapStyle)) {
                this.setTileSource(source);
                iSourceButton.setCurrentMap(source);
                foundSource = true;
                break;
            }
        }
        if (!foundSource) {
            setTileSource(tileSources.get(0));
            iSourceButton.setCurrentMap(tileSources.get(0));
        }

        MainApplication.getLayerManager().addActiveLayerChangeListener(this);

        new SlippyMapControler(this, this);
    }

    private static LinkedHashMap<String, TileSource> getAllTileSources() {
        // using a LinkedHashMap of <id, TileSource> to retain ordering but provide deduplication
        return providers.stream().flatMap(
            provider -> provider.getTileSources().stream()
        ).collect(Collectors.toMap(
            TileSource::getId,
            ts -> ts,
            (oldTs, newTs) -> oldTs,
            LinkedHashMap::new
        ));
    }

    /**
     * Handles a click/move on the attribution
     * @param p The point in the view
     * @param click true if it was a click, false for hover
     * @return if the attribution handled the event
     */
    public boolean handleAttribution(Point p, boolean click) {
        return attribution.handleAttribution(p, click);
    }

    /**
     * Draw the map.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // draw shaded area for non-downloaded region of current data set, but only if there *is* a current data set,
        // and it has defined bounds. Routine is analogous to that in OsmDataLayer's paint routine (but just different
        // enough to make sharing code impractical)
        final DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
        if (ds != null && this.showDownloadAreaButtonModel.isSelected() && !ds.getDataSources().isEmpty()) {
            // initialize area with current viewport
            Rectangle b = this.getBounds();
            // ensure we comfortably cover full area
            b.grow(100, 100);
            Path2D p = new Path2D.Float();

            // combine successively downloaded areas after converting to screen-space
            for (Bounds bounds : ds.getDataSourceBounds()) {
                if (bounds.isCollapsed()) {
                    continue;
                }
                Rectangle r = new Rectangle(this.getMapPosition(bounds.getMinLat(), bounds.getMinLon(), false));
                r.add(this.getMapPosition(bounds.getMaxLat(), bounds.getMaxLon(), false));
                p.append(r, false);
            }
            // subtract combined areas
            Area a = new Area(b);
            a.subtract(new Area(p));

            // paint remainder
            g2d.setPaint(new Color(0, 0, 0, 32));
            g2d.fill(a);
        }

        // draw selection rectangle
        if (iSelectionRectStart != null && iSelectionRectEnd != null) {
            Rectangle box = new Rectangle(getMapPosition(iSelectionRectStart, false));
            box.add(getMapPosition(iSelectionRectEnd, false));

            g.setColor(new Color(0.9f, 0.7f, 0.7f, 0.6f));
            g.fillRect(box.x, box.y, box.width, box.height);

            g.setColor(Color.BLACK);
            g.drawRect(box.x, box.y, box.width, box.height);
        }
    }

    @Override
    public void activeOrEditLayerChanged(MainLayerManager.ActiveLayerChangeEvent e) {
        this.repaint();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // fired for the stateChanged event of this.showDownloadAreaButtonModel
        PROP_SHOWDLAREA.put(this.showDownloadAreaButtonModel.isSelected());
        this.repaint();
    }

    /**
     * Enables the disk tile cache.
     * @param enabled true to enable, false to disable
     */
    public final void setFileCacheEnabled(boolean enabled) {
        if (enabled && cachedLoader != null) {
            setTileLoader(cachedLoader);
        } else {
            setTileLoader(uncachedLoader);
        }
    }

    /**
     * Sets the maximum number of tiles that may be held in memory
     * @param tiles The maximum number of tiles.
     */
    public final void setMaxTilesInMemory(int tiles) {
        ((MemoryTileCache) getTileCache()).setCacheSize(tiles);
    }

    /**
     * Callback for the OsmMapControl. (Re-)Sets the start and end point of the selection rectangle.
     *
     * @param aStart selection start
     * @param aEnd selection end
     */
    public void setSelection(Point aStart, Point aEnd) {
        if (aStart == null || aEnd == null || aStart.x == aEnd.x || aStart.y == aEnd.y)
            return;

        Point pMax = new Point(Math.max(aEnd.x, aStart.x), Math.max(aEnd.y, aStart.y));
        Point pMin = new Point(Math.min(aEnd.x, aStart.x), Math.min(aEnd.y, aStart.y));

        iSelectionRectStart = getPosition(pMin);
        iSelectionRectEnd = getPosition(pMax);

        Bounds b = new Bounds(
                new LatLon(
                        Math.min(iSelectionRectStart.getLat(), iSelectionRectEnd.getLat()),
                        LatLon.toIntervalLon(Math.min(iSelectionRectStart.getLon(), iSelectionRectEnd.getLon()))
                        ),
                        new LatLon(
                                Math.max(iSelectionRectStart.getLat(), iSelectionRectEnd.getLat()),
                                LatLon.toIntervalLon(Math.max(iSelectionRectStart.getLon(), iSelectionRectEnd.getLon())))
                );
        Bounds oldValue = this.bbox;
        this.bbox = b;
        repaint();
        firePropertyChange(BBOX_PROP, oldValue, this.bbox);
    }

    /**
     * Performs resizing of the DownloadDialog in order to enlarge or shrink the
     * map.
     */
    public void resizeSlippyMap() {
        boolean large = iSizeButton.isEnlarged();
        firePropertyChange(RESIZE_PROP, !large, large);
    }

    /**
     * Sets the active tile source
     * @param tileSource The active tile source
     */
    public void toggleMapSource(TileSource tileSource) {
        this.tileController.setTileCache(new MemoryTileCache());
        this.setTileSource(tileSource);
        PROP_MAPSTYLE.put(tileSource.getName()); // TODO Is name really unique?

        // we need to refresh the tile sources in case the deselected source should no longer be present
        // (and only remained there because its removal was deferred while the source was still the
        // selected one). this should also have the effect of propagating the new selection to the
        // iSourceButton & menu: it attempts to re-select the current source when rebuilding its menu.
        this.refreshTileSources();
    }

    @Override
    public Bounds getBoundingBox() {
        return bbox;
    }

    /**
     * Sets the current bounding box in this bbox chooser without
     * emitting a property change event.
     *
     * @param bbox the bounding box. null to reset the bounding box
     */
    @Override
    public void setBoundingBox(Bounds bbox) {
        if (bbox == null || (bbox.getMinLat() == 0 && bbox.getMinLon() == 0
                && bbox.getMaxLat() == 0 && bbox.getMaxLon() == 0)) {
            this.bbox = null;
            iSelectionRectStart = null;
            iSelectionRectEnd = null;
            repaint();
            return;
        }

        this.bbox = bbox;
        iSelectionRectStart = new Coordinate(bbox.getMinLat(), bbox.getMinLon());
        iSelectionRectEnd = new Coordinate(bbox.getMaxLat(), bbox.getMaxLon());

        // calc the screen coordinates for the new selection rectangle
        MapMarkerDot min = new MapMarkerDot(bbox.getMinLat(), bbox.getMinLon());
        MapMarkerDot max = new MapMarkerDot(bbox.getMaxLat(), bbox.getMaxLon());

        List<MapMarker> marker = new ArrayList<>(2);
        marker.add(min);
        marker.add(max);
        setMapMarkerList(marker);
        setDisplayToFitMapMarkers();
        zoomOut();
        repaint();
    }

    /**
     * Enables or disables painting of the shrink/enlarge button
     *
     * @param visible {@code true} to enable painting of the shrink/enlarge button
     */
    public void setSizeButtonVisible(boolean visible) {
        iSizeButton.setVisible(visible);
    }

    /**
     * Refreshes the tile sources
     * @since 6364
     */
    public final void refreshTileSources() {
        final LinkedHashMap<String, TileSource> newTileSources = getAllTileSources();
        final TileSource currentTileSource = this.getTileController().getTileSource();

        // re-add the currently active TileSource to prevent inconsistent display of menu
        newTileSources.putIfAbsent(currentTileSource.getId(), currentTileSource);

        this.iSourceButton.setSources(new ArrayList<>(newTileSources.values()));
    }

    @Override
    public void layerAdded(MainLayerManager.LayerAddEvent e) {
        if (e.getAddedLayer() instanceof ImageryLayer) {
            this.refreshTileSources();
        }
    }

    @Override
    public void layerRemoving(MainLayerManager.LayerRemoveEvent e) {
        if (e.getRemovedLayer() instanceof ImageryLayer) {
            this.refreshTileSources();
        }
    }

    @Override
    public void layerOrderChanged(MainLayerManager.LayerOrderChangeEvent e) {}
}
