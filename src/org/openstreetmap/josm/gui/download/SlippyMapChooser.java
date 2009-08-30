// This code has been adapted and copied from code that has been written by Immanuel Scholz and others for JOSM.
// License: GPL. Copyright 2007 by Tim Haussmann
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.OsmFileCacheTileLoader;
import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.OsmTileSource;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.Main;

/**
 * JComponent that displays the slippy map tiles
 *
 * @author Tim Haussmann
 *
 */
public class SlippyMapChooser extends JMapViewer implements DownloadSelection {

    private DownloadDialog iGui;

    // upper left and lower right corners of the selection rectangle (x/y on
    // ZOOM_MAX)
    Point iSelectionRectStart;
    Point iSelectionRectEnd;

    private SizeButton iSizeButton = new SizeButton();
    private SourceButton iSourceButton = new SourceButton();

    // standard dimension
    private Dimension iDownloadDialogDimension;
    // screen size
    private Dimension iScreenSize;

    private TileSource[] sources = { new OsmTileSource.Mapnik(), new OsmTileSource.TilesAtHome(),
            new OsmTileSource.CycleMap() };
    TileLoader cachedLoader;
    TileLoader uncachedLoader;
    JPanel slipyyMapTabPanel;

    /**
     * Create the chooser component.
     */
    public SlippyMapChooser() {
        super();
        try {
            cachedLoader = new OsmFileCacheTileLoader(this);
        } catch(SecurityException e) {
            // set to null if a SecurityException was thrown
            // while creating the cachedLoader
            //
            cachedLoader = null;
        }
        uncachedLoader = new OsmTileLoader(this);
        setZoomContolsVisible(false);
        setMapMarkerVisible(false);
        setMinimumSize(new Dimension(350, 350 / 2));
        // We need to set an initial size - this prevents a wrong zoom selection for
        // the area before the component has been displayed the first time
        setBounds(new Rectangle(getMinimumSize()));
        if (cachedLoader == null) {
            setFileCacheEnabled(false);
        } else {
            setFileCacheEnabled(Main.pref.getBoolean("slippy_map_chooser.file_cache", true));
        }
        setMaxTilesInMemory(Main.pref.getInteger("slippy_map_chooser.max_tiles", 1000));

        String mapStyle = Main.pref.get("slippy_map_chooser.mapstyle", "mapnik");
        if (mapStyle.equals("osmarender")) {
            iSourceButton.setMapStyle(SourceButton.OSMARENDER);
            this.setTileSource(sources[1]);
        } else if (mapStyle.equals("cyclemap")) {
            iSourceButton.setMapStyle(SourceButton.CYCLEMAP);
            this.setTileSource(sources[2]);
        } else {
            if (!mapStyle.equals("mapnik")) {
                Main.pref.put("slippy_map_chooser.mapstyle", "mapnik");
            }
        }
    }

    public void setMaxTilesInMemory(int tiles) {
        ((MemoryTileCache) getTileCache()).setCacheSize(tiles);
    }

    public void setFileCacheEnabled(boolean enabled) {
        if (enabled) {
            setTileLoader(cachedLoader);
        } else {
            setTileLoader(uncachedLoader);
        }
    }

    public void addGui(final DownloadDialog gui) {
        iGui = gui;
        slipyyMapTabPanel = new JPanel();
        slipyyMapTabPanel.setLayout(new BorderLayout());
        slipyyMapTabPanel.add(this, BorderLayout.CENTER);
        String labelText = tr("<b>Zoom:</b> Mousewheel, double click or Ctrl + Up/Down "
                + "<b>Move map:</b> Hold right mousebutton and move mouse or use cursor keys. <b>Select:</b> Click.");
        slipyyMapTabPanel.add(new JLabel("<html>" + labelText + "</html>"), BorderLayout.SOUTH);
        iGui.tabpane.add(slipyyMapTabPanel, tr("Slippy map"));
        new OsmMapControl(this, slipyyMapTabPanel, iSizeButton, iSourceButton);
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
            iSourceButton.paint(g);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void boundingBoxChanged(DownloadDialog gui) {

        // test if a bounding box has been set set
        if (gui.minlat == 0.0 && gui.minlon == 0.0 && gui.maxlat == 0.0 && gui.maxlon == 0.0)
            return;

        int y1 = OsmMercator.LatToY(gui.minlat, MAX_ZOOM);
        int y2 = OsmMercator.LatToY(gui.maxlat, MAX_ZOOM);
        int x1 = OsmMercator.LonToX(gui.minlon, MAX_ZOOM);
        int x2 = OsmMercator.LonToX(gui.maxlon, MAX_ZOOM);

        iSelectionRectStart = new Point(Math.min(x1, x2), Math.min(y1, y2));
        iSelectionRectEnd = new Point(Math.max(x1, x2), Math.max(y1, y2));

        // calc the screen coordinates for the new selection rectangle
        MapMarkerDot xmin_ymin = new MapMarkerDot(gui.minlat, gui.minlon);
        MapMarkerDot xmax_ymax = new MapMarkerDot(gui.maxlat, gui.maxlon);

        Vector<MapMarker> marker = new Vector<MapMarker>(2);
        marker.add(xmin_ymin);
        marker.add(xmax_ymax);
        setMapMarkerList(marker);
        setDisplayToFitMapMarkers();
        zoomOut();
    }

    /**
     * Callback for the OsmMapControl. (Re-)Sets the start and end point of the
     * selection rectangle.
     *
     * @param aStart
     * @param aEnd
     */
    public void setSelection(Point aStart, Point aEnd) {
        if (aStart == null || aEnd == null)
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

        Coordinate l1 = getPosition(p_max);
        Coordinate l2 = getPosition(p_min);
        iGui.minlon = Math.min(l2.getLon(), l1.getLon());
        iGui.minlat = Math.min(l1.getLat(), l2.getLat());
        iGui.maxlon = Math.max(l2.getLon(), l1.getLon());
        iGui.maxlat = Math.max(l1.getLat(), l2.getLat());

        iGui.boundingBoxChanged(this);
        repaint();
    }

    /**
     * Performs resizing of the DownloadDialog in order to enlarge or shrink the
     * map.
     */
    public void resizeSlippyMap() {
        if (iScreenSize == null) {
            Component c = iGui.getParent().getParent().getParent().getParent().getParent().getParent().getParent()
            .getParent().getParent();
            // remember the initial set screen dimensions
            iDownloadDialogDimension = c.getSize();
            // retrive the size of the display
            iScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        }

        // resize
        Component co = iGui.getParent().getParent().getParent().getParent().getParent().getParent().getParent()
        .getParent().getParent();
        Dimension currentDimension = co.getSize();

        // enlarge
        if (currentDimension.equals(iDownloadDialogDimension)) {
            // make the each dimension 90% of the absolute display size and
            // center the DownloadDialog
            int w = iScreenSize.width * 90 / 100;
            int h = iScreenSize.height * 90 / 100;
            co.setBounds((iScreenSize.width - w) / 2, (iScreenSize.height - h) / 2, w, h);

        }
        // shrink
        else {
            // set the size back to the initial dimensions and center the
            // DownloadDialog
            int w = iDownloadDialogDimension.width;
            int h = iDownloadDialogDimension.height;
            co.setBounds((iScreenSize.width - w) / 2, (iScreenSize.height - h) / 2, w, h);

        }

        repaint();
    }

    public void toggleMapSource(int mapSource) {
        this.tileController.setTileCache(new MemoryTileCache());
        if (mapSource == SourceButton.MAPNIK) {
            this.setTileSource(sources[0]);
            Main.pref.put("slippy_map_chooser.mapstyle", "mapnik");
        } else if (mapSource == SourceButton.CYCLEMAP) {
            this.setTileSource(sources[2]);
            Main.pref.put("slippy_map_chooser.mapstyle", "cyclemap");
        } else {
            this.setTileSource(sources[1]);
            Main.pref.put("slippy_map_chooser.mapstyle", "osmarender");
        }
    }

}
