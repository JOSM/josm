// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.bbox;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JOptionPane;
import javax.swing.SpringLayout;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOpenAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOsmTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.layer.TMSLayer;

public class SlippyMapBBoxChooser extends JMapViewer implements BBoxChooser {

    public interface TileSourceProvider {
        List<TileSource> getTileSources();
    }

    /**
     * TMS TileSource provider for the slippymap chooser
     */
    public static class TMSTileSourceProvider implements TileSourceProvider {
        static final Set<String> existingSlippyMapUrls = new HashSet<String>();
        static {
            // Urls that already exist in the slippymap chooser and shouldn't be copied from TMS layer list
            existingSlippyMapUrls.add("http://tile.openstreetmap.org/{zoom}/{x}/{y}.png");      // Mapnik
            existingSlippyMapUrls.add("http://tile.opencyclemap.org/cycle/{zoom}/{x}/{y}.png"); // Cyclemap
            existingSlippyMapUrls.add("http://otile{switch:1,2,3,4}.mqcdn.com/tiles/1.0.0/osm/{zoom}/{x}/{y}.png"); // MapQuest-OSM
            existingSlippyMapUrls.add("http://oatile{switch:1,2,3,4}.mqcdn.com/tiles/1.0.0/sat/{zoom}/{x}/{y}.png"); // MapQuest Open Aerial
        }

        @Override
        public List<TileSource> getTileSources() {
            if (!TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get()) return Collections.<TileSource>emptyList();
            List<TileSource> sources = new ArrayList<TileSource>();
            for (ImageryInfo info : ImageryLayerInfo.instance.getLayers()) {
                if (existingSlippyMapUrls.contains(info.getUrl())) {
                    continue;
                }
                try {
                    TileSource source = TMSLayer.getTileSource(info);
                    if (source != null) {
                        sources.add(source);
                    }
                } catch (IllegalArgumentException ex) {
                    if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                        JOptionPane.showMessageDialog(Main.parent,
                                ex.getMessage(), tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
            return sources;
        }

        public static void addExistingSlippyMapUrl(String url) {
            existingSlippyMapUrls.add(url);
        }
    }

    /**
     * Plugins that wish to add custom tile sources to slippy map choose should call this method
     * @param tileSourceProvider
     */
    public static void addTileSourceProvider(TileSourceProvider tileSourceProvider) {
        providers.addIfAbsent(tileSourceProvider);
    }

    private static CopyOnWriteArrayList<TileSourceProvider> providers = new CopyOnWriteArrayList<TileSourceProvider>();

    static {
        addTileSourceProvider(new TileSourceProvider() {
            @Override
            public List<TileSource> getTileSources() {
                return Arrays.<TileSource>asList(
                        new OsmTileSource.Mapnik(),
                        new OsmTileSource.CycleMap(),
                        new MapQuestOsmTileSource(),
                        new MapQuestOpenAerialTileSource());
            }
        });
        addTileSourceProvider(new TMSTileSourceProvider());
    }

    private static final StringProperty PROP_MAPSTYLE = new StringProperty("slippy_map_chooser.mapstyle", "Mapnik");
    public static final String RESIZE_PROP = SlippyMapBBoxChooser.class.getName() + ".resize";

    private OsmTileLoader cachedLoader;
    private OsmTileLoader uncachedLoader;

    private final SizeButton iSizeButton;
    private final SourceButton iSourceButton;
    private Bounds bbox;

    // upper left and lower right corners of the selection rectangle (x/y on ZOOM_MAX)
    Point iSelectionRectStart;
    Point iSelectionRectEnd;

    /**
     * Constructs a new {@code SlippyMapBBoxChooser}.
     */
    public SlippyMapBBoxChooser() {
        SpringLayout springLayout = new SpringLayout();
        setLayout(springLayout);
        TMSLayer.setMaxWorkers();
        cachedLoader = TMSLayer.loaderFactory.makeTileLoader(this);

        uncachedLoader = new OsmTileLoader(this);
        uncachedLoader.headers.put("User-Agent", Version.getInstance().getFullAgentString());
        setZoomContolsVisible(Main.pref.getBoolean("slippy_map_chooser.zoomcontrols",false));
        setMapMarkerVisible(false);
        setMinimumSize(new Dimension(350, 350 / 2));
        // We need to set an initial size - this prevents a wrong zoom selection
        // for the area before the component has been displayed the first time
        setBounds(new Rectangle(getMinimumSize()));
        if (cachedLoader == null) {
            setFileCacheEnabled(false);
        } else {
            setFileCacheEnabled(Main.pref.getBoolean("slippy_map_chooser.file_cache", true));
        }
        setMaxTilesInMemory(Main.pref.getInteger("slippy_map_chooser.max_tiles", 1000));

        List<TileSource> tileSources = getAllTileSources();

        iSourceButton = new SourceButton(this, tileSources);
        add(iSourceButton);
        springLayout.putConstraint(SpringLayout.EAST, iSourceButton, 0, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.NORTH, iSourceButton, 30, SpringLayout.NORTH, this);

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

        new SlippyMapControler(this, this);
    }

    private List<TileSource> getAllTileSources() {
        List<TileSource> tileSources = new ArrayList<TileSource>();
        for (TileSourceProvider provider: providers) {
            tileSources.addAll(provider.getTileSources());
        }
        return tileSources;
    }

    public boolean handleAttribution(Point p, boolean click) {
        return attribution.handleAttribution(p, click);
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

            // draw selection rectangle
            if (iSelectionRectStart != null && iSelectionRectEnd != null) {

                int zoomDiff = MAX_ZOOM - zoom;
                Point tlc = getTopLeftCoordinates();
                int x_min = (iSelectionRectStart.x >> zoomDiff) - tlc.x;
                int y_min = (iSelectionRectStart.y >> zoomDiff) - tlc.y;
                int x_max = (iSelectionRectEnd.x >> zoomDiff) - tlc.x;
                int y_max = (iSelectionRectEnd.y >> zoomDiff) - tlc.y;

                int w = x_max - x_min;
                int h = y_max - y_min;
                g.setColor(new Color(0.9f, 0.7f, 0.7f, 0.6f));
                g.fillRect(x_min, y_min, w, h);

                g.setColor(Color.BLACK);
                g.drawRect(x_min, y_min, w, h);
            }
        } catch (Exception e) {
            Main.error(e);
        }
    }

    public void setFileCacheEnabled(boolean enabled) {
        if (enabled) {
            setTileLoader(cachedLoader);
        } else {
            setTileLoader(uncachedLoader);
        }
    }

    public void setMaxTilesInMemory(int tiles) {
        ((MemoryTileCache) getTileCache()).setCacheSize(tiles);
    }


    /**
     * Callback for the OsmMapControl. (Re-)Sets the start and end point of the
     * selection rectangle.
     *
     * @param aStart
     * @param aEnd
     */
    public void setSelection(Point aStart, Point aEnd) {
        if (aStart == null || aEnd == null || aStart.x == aEnd.x || aStart.y == aEnd.y)
            return;

        Point p_max = new Point(Math.max(aEnd.x, aStart.x), Math.max(aEnd.y, aStart.y));
        Point p_min = new Point(Math.min(aEnd.x, aStart.x), Math.min(aEnd.y, aStart.y));

        Point tlc = getTopLeftCoordinates();
        int zoomDiff = MAX_ZOOM - zoom;
        Point pEnd = new Point(p_max.x + tlc.x, p_max.y + tlc.y);
        Point pStart = new Point(p_min.x + tlc.x, p_min.y + tlc.y);

        pEnd.x <<= zoomDiff;
        pEnd.y <<= zoomDiff;
        pStart.x <<= zoomDiff;
        pStart.y <<= zoomDiff;

        iSelectionRectStart = pStart;
        iSelectionRectEnd = pEnd;

        Coordinate l1 = getPosition(p_max); // lon may be outside [-180,180]
        Coordinate l2 = getPosition(p_min); // lon may be outside [-180,180]
        Bounds b = new Bounds(
                new LatLon(
                        Math.min(l2.getLat(), l1.getLat()),
                        LatLon.toIntervalLon(Math.min(l1.getLon(), l2.getLon()))
                        ),
                        new LatLon(
                                Math.max(l2.getLat(), l1.getLat()),
                                LatLon.toIntervalLon(Math.max(l1.getLon(), l2.getLon())))
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

    public void toggleMapSource(TileSource tileSource) {
        this.tileController.setTileCache(new MemoryTileCache());
        this.setTileSource(tileSource);
        PROP_MAPSTYLE.put(tileSource.getName()); // TODO Is name really unique?
    }

    @Override
    public Bounds getBoundingBox() {
        return bbox;
    }

    /**
     * Sets the current bounding box in this bbox chooser without
     * emiting a property change event.
     *
     * @param bbox the bounding box. null to reset the bounding box
     */
    @Override
    public void setBoundingBox(Bounds bbox) {
        if (bbox == null || (bbox.getMinLat() == 0.0 && bbox.getMinLon() == 0.0
                && bbox.getMaxLat() == 0.0 && bbox.getMaxLon() == 0.0)) {
            this.bbox = null;
            iSelectionRectStart = null;
            iSelectionRectEnd = null;
            repaint();
            return;
        }

        this.bbox = bbox;
        double minLon = bbox.getMinLon();
        double maxLon = bbox.getMaxLon();

        if (bbox.crosses180thMeridian()) {
            minLon -= 360.0;
        }

        int y1 = tileSource.LatToY(bbox.getMinLat(), MAX_ZOOM);
        int y2 = tileSource.LatToY(bbox.getMaxLat(), MAX_ZOOM);
        int x1 = tileSource.LonToX(minLon, MAX_ZOOM);
        int x2 = tileSource.LonToX(maxLon, MAX_ZOOM);

        iSelectionRectStart = new Point(Math.min(x1, x2), Math.min(y1, y2));
        iSelectionRectEnd = new Point(Math.max(x1, x2), Math.max(y1, y2));

        // calc the screen coordinates for the new selection rectangle
        MapMarkerDot xmin_ymin = new MapMarkerDot(bbox.getMinLat(), bbox.getMinLon());
        MapMarkerDot xmax_ymax = new MapMarkerDot(bbox.getMaxLat(), bbox.getMaxLon());

        List<MapMarker> marker = new ArrayList<MapMarker>(2);
        marker.add(xmin_ymin);
        marker.add(xmax_ymax);
        setMapMarkerList(marker);
        setDisplayToFitMapMarkers();
        zoomOut();
        repaint();
    }

    /**
     * Refreshes the tile sources
     * @since 6364
     */
    public final void refreshTileSources() {
        iSourceButton.setSources(getAllTileSources());
    }
}
