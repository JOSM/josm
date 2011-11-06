// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.bbox;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JOptionPane;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmFileCacheTileLoader;
import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.layer.TMSLayer;

public class SlippyMapBBoxChooser extends JMapViewer implements BBoxChooser{

    public interface TileSourceProvider {
        List<TileSource> getTileSources();
    }

    public static class RenamedSourceDecorator implements TileSource {

        private final TileSource source;
        private final String name;

        public RenamedSourceDecorator(TileSource source, String name) {
            this.source = source;
            this.name = name;
        }

        @Override public String getName() {
            return name;
        }

        @Override public int getMaxZoom() { return source.getMaxZoom(); }

        @Override public int getMinZoom() { return source.getMinZoom(); }

        @Override public int getTileSize() { return source.getTileSize(); }

        @Override public String getTileType() { return source.getTileType(); }

        @Override public TileUpdate getTileUpdate() { return source.getTileUpdate(); }

        @Override public String getTileUrl(int zoom, int tilex, int tiley) throws IOException { return source.getTileUrl(zoom, tilex, tiley); }

        @Override public boolean requiresAttribution() { return source.requiresAttribution(); }

        @Override public String getAttributionText(int zoom, Coordinate topLeft, Coordinate botRight) { return source.getAttributionText(zoom, topLeft, botRight); }

        @Override public String getAttributionLinkURL() { return source.getAttributionLinkURL(); }

        @Override public Image getAttributionImage() { return source.getAttributionImage(); }

        @Override public String getAttributionImageURL() { return source.getAttributionImageURL(); }

        @Override public String getTermsOfUseText() { return source.getTermsOfUseText(); }

        @Override public String getTermsOfUseURL() { return source.getTermsOfUseURL(); }

        @Override public double latToTileY(double lat, int zoom) { return source.latToTileY(lat,zoom); }

        @Override public double lonToTileX(double lon, int zoom) { return source.lonToTileX(lon,zoom); }

        @Override public double tileYToLat(int y, int zoom) { return tileYToLat(y, zoom); }

        @Override public double tileXToLon(int x, int zoom) { return tileXToLon(x, zoom); }
    }

    /**
     * TMS TileSource provider for the slippymap chooser
     */
    public static class TMSTileSourceProvider implements TileSourceProvider {
        static final HashSet<String> existingSlippyMapUrls = new HashSet<String>();
        static {
            // Urls that already exist in the slippymap chooser and shouldn't be copied from TMS layer list
            existingSlippyMapUrls.add("http://tile.openstreetmap.org/");
            existingSlippyMapUrls.add("http://tah.openstreetmap.org/Tiles/tile/");
            existingSlippyMapUrls.add("http://tile.opencyclemap.org/cycle/");
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
                        new RenamedSourceDecorator(new OsmTileSource.Mapnik(), "Mapnik"),
                        new RenamedSourceDecorator(new OsmTileSource.TilesAtHome(), "Osmarender"),
                        new RenamedSourceDecorator(new OsmTileSource.CycleMap(), "Cyclemap")
                );
            }
        });
        addTileSourceProvider(new TMSTileSourceProvider());
    }

    private static final StringProperty PROP_MAPSTYLE = new StringProperty("slippy_map_chooser.mapstyle", "Mapnik");
    public static final String RESIZE_PROP = SlippyMapBBoxChooser.class.getName() + ".resize";

    private TileLoader cachedLoader;
    private TileLoader uncachedLoader;

    private final SizeButton iSizeButton = new SizeButton();
    private final SourceButton iSourceButton;
    private Bounds bbox;

    // upper left and lower right corners of the selection rectangle (x/y on
    // ZOOM_MAX)
    Point iSelectionRectStart;
    Point iSelectionRectEnd;

    public SlippyMapBBoxChooser() {
        super();
        cachedLoader = null;
        String cachePath = TMSLayer.PROP_TILECACHE_DIR.get();
        if (cachePath != null && !cachePath.isEmpty()) {
            try {
                cachedLoader = new OsmFileCacheTileLoader(this, new File(cachePath));
            } catch (IOException e) {
            }
        }

        uncachedLoader = new OsmTileLoader(this);
        setZoomContolsVisible(false);
        setMapMarkerVisible(false);
        setMinimumSize(new Dimension(350, 350 / 2));
        // We need to set an initial size - this prevents a wrong zoom selection
        // for
        // the area before the component has been displayed the first time
        setBounds(new Rectangle(getMinimumSize()));
        if (cachedLoader == null) {
            setFileCacheEnabled(false);
        } else {
            setFileCacheEnabled(Main.pref.getBoolean("slippy_map_chooser.file_cache", true));
        }
        setMaxTilesInMemory(Main.pref.getInteger("slippy_map_chooser.max_tiles", 1000));

        List<TileSource> tileSources = new ArrayList<TileSource>();
        for (TileSourceProvider provider: providers) {
            tileSources.addAll(provider.getTileSources());
        }

        iSourceButton = new SourceButton(tileSources);

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

        new SlippyMapControler(this, this, iSizeButton, iSourceButton);
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

            iSizeButton.paint(g);
            iSourceButton.paint((Graphics2D)g);
        } catch (Exception e) {
            e.printStackTrace();
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

    public Bounds getBoundingBox() {
        return bbox;
    }

    /**
     * Sets the current bounding box in this bbox chooser without
     * emiting a property change event.
     *
     * @param bbox the bounding box. null to reset the bounding box
     */
    public void setBoundingBox(Bounds bbox) {
        if (bbox == null || (bbox.getMin().lat() == 0.0 && bbox.getMin().lon() == 0.0
        && bbox.getMax().lat() == 0.0 && bbox.getMax().lon() == 0.0)) {
            this.bbox = null;
            iSelectionRectStart = null;
            iSelectionRectEnd = null;
            repaint();
            return;
        }

        this.bbox = bbox;
        double minLon = bbox.getMin().lon();
        double maxLon = bbox.getMax().lon();
        
        if (bbox.crosses180thMeridian()) {
            minLon -= 360.0;
        }

        int y1 = OsmMercator.LatToY(bbox.getMin().lat(), MAX_ZOOM);
        int y2 = OsmMercator.LatToY(bbox.getMax().lat(), MAX_ZOOM);
        int x1 = OsmMercator.LonToX(minLon, MAX_ZOOM);
        int x2 = OsmMercator.LonToX(maxLon, MAX_ZOOM);
        
        iSelectionRectStart = new Point(Math.min(x1, x2), Math.min(y1, y2));
        iSelectionRectEnd = new Point(Math.max(x1, x2), Math.max(y1, y2));

        // calc the screen coordinates for the new selection rectangle
        MapMarkerDot xmin_ymin = new MapMarkerDot(bbox.getMin().lat(), bbox.getMin().lon());
        MapMarkerDot xmax_ymax = new MapMarkerDot(bbox.getMax().lat(), bbox.getMax().lon());

        Vector<MapMarker> marker = new Vector<MapMarker>(2);
        marker.add(xmin_ymin);
        marker.add(xmax_ymax);
        setMapMarkerList(marker);
        setDisplayToFitMapMarkers();
        zoomOut();
        repaint();
    }
}
