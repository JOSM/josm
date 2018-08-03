// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.preferences.CachedProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.template_engine.ParseError;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;
import org.openstreetmap.josm.tools.template_engine.TemplateEntry;
import org.openstreetmap.josm.tools.template_engine.TemplateParser;

/**
 * Basic marker class. Requires a position, and supports
 * a custom icon and a name.
 *
 * This class is also used to create appropriate Marker-type objects
 * when waypoints are imported.
 *
 * It hosts a public list object, named makers, containing implementations of
 * the MarkerMaker interface. Whenever a Marker needs to be created, each
 * object in makers is called with the waypoint parameters (Lat/Lon and tag
 * data), and the first one to return a Marker object wins.
 *
 * By default, one the list contains one default "Maker" implementation that
 * will create AudioMarkers for supported audio files, ImageMarkers for supported image
 * files, and WebMarkers for everything else. (The creation of a WebMarker will
 * fail if there's no valid URL in the &lt;link&gt; tag, so it might still make sense
 * to add Makers for such waypoints at the end of the list.)
 *
 * The default implementation only looks at the value of the &lt;link&gt; tag inside
 * the &lt;wpt&gt; tag of the GPX file.
 *
 * <h2>HowTo implement a new Marker</h2>
 * <ul>
 * <li> Subclass Marker or ButtonMarker and override <code>containsPoint</code>
 *      if you like to respond to user clicks</li>
 * <li> Override paint, if you want a custom marker look (not "a label and a symbol")</li>
 * <li> Implement MarkerCreator to return a new instance of your marker class</li>
 * <li> In you plugin constructor, add an instance of your MarkerCreator
 *      implementation either on top or bottom of Marker.markerProducers.
 *      Add at top, if your marker should overwrite an current marker or at bottom
 *      if you only add a new marker style.</li>
 * </ul>
 *
 * @author Frederik Ramm
 */
public class Marker implements TemplateEngineDataProvider, ILatLon {

    public static final class TemplateEntryProperty extends CachedProperty<TemplateEntry> {
        // This class is a bit complicated because it supports both global and per layer settings. I've added per layer settings because
        // GPXSettingsPanel had possibility to set waypoint label but then I've realized that markers use different layer then gpx data
        // so per layer settings is useless. Anyway it's possible to specify marker layer pattern in Einstein preferences and maybe somebody
        // will make gui for it so I'm keeping it here

        private static final Map<String, TemplateEntryProperty> CACHE = new HashMap<>();

        public static TemplateEntryProperty forMarker(String layerName) {
            String key = "draw.rawgps.layer.wpt.pattern";
            if (layerName != null) {
                key += '.' + layerName;
            }
            TemplateEntryProperty result = CACHE.get(key);
            if (result == null) {
                String defaultValue = layerName == null ? LABEL_PATTERN_AUTO : "";
                TemplateEntryProperty parent = layerName == null ? null : forMarker(null);
                result = new TemplateEntryProperty(key, defaultValue, parent);
                CACHE.put(key, result);
            }
            return result;
        }

        public static TemplateEntryProperty forAudioMarker(String layerName) {
            String key = "draw.rawgps.layer.audiowpt.pattern";
            if (layerName != null) {
                key += '.' + layerName;
            }
            TemplateEntryProperty result = CACHE.get(key);
            if (result == null) {
                String defaultValue = layerName == null ? "?{ '{name}' | '{desc}' | '{" + Marker.MARKER_FORMATTED_OFFSET + "}' }" : "";
                TemplateEntryProperty parent = layerName == null ? null : forAudioMarker(null);
                result = new TemplateEntryProperty(key, defaultValue, parent);
                CACHE.put(key, result);
            }
            return result;
        }

        private final TemplateEntryProperty parent;

        private TemplateEntryProperty(String key, String defaultValue, TemplateEntryProperty parent) {
            super(key, defaultValue);
            this.parent = parent;
            updateValue(); // Needs to be called because parent wasn't know in super constructor
        }

        @Override
        protected TemplateEntry fromString(String s) {
            try {
                return new TemplateParser(s).parse();
            } catch (ParseError e) {
                Logging.debug(e);
                Logging.warn("Unable to parse template engine pattern ''{0}'' for property {1}. Using default (''{2}'') instead",
                        s, getKey(), super.getDefaultValueAsString());
                return getDefaultValue();
            }
        }

        @Override
        public String getDefaultValueAsString() {
            if (parent == null)
                return super.getDefaultValueAsString();
            else
                return parent.getAsString();
        }

        @Override
        public void preferenceChanged(PreferenceChangeEvent e) {
            if (e.getKey().equals(key) || (parent != null && e.getKey().equals(parent.getKey()))) {
                updateValue();
            }
        }
    }

    /**
     * Plugins can add their Marker creation stuff at the bottom or top of this list
     * (depending on whether they want to override default behaviour or just add new stuff).
     */
    private static final List<MarkerProducers> markerProducers = new LinkedList<>();

    // Add one Marker specifying the default behaviour.
    static {
        Marker.markerProducers.add(new DefaultMarkerProducers());
    }

    /**
     * Add a new marker producers at the end of the JOSM list.
     * @param mp a new marker producers
     * @since 11850
     */
    public static void appendMarkerProducer(MarkerProducers mp) {
        markerProducers.add(mp);
    }

    /**
     * Add a new marker producers at the beginning of the JOSM list.
     * @param mp a new marker producers
     * @since 11850
     */
    public static void prependMarkerProducer(MarkerProducers mp) {
        markerProducers.add(0, mp);
    }

    /**
     * Returns an object of class Marker or one of its subclasses
     * created from the parameters given.
     *
     * @param wpt waypoint data for marker
     * @param relativePath An path to use for constructing relative URLs or
     *        <code>null</code> for no relative URLs
     * @param parentLayer the <code>MarkerLayer</code> that will contain the created <code>Marker</code>
     * @param time time of the marker in seconds since epoch
     * @param offset double in seconds as the time offset of this marker from
     *        the GPX file from which it was derived (if any).
     * @return a new Marker object
     */
    public static Collection<Marker> createMarkers(WayPoint wpt, File relativePath, MarkerLayer parentLayer, double time, double offset) {
        for (MarkerProducers maker : Marker.markerProducers) {
            final Collection<Marker> markers = maker.createMarkers(wpt, relativePath, parentLayer, time, offset);
            if (markers != null)
                return markers;
        }
        return null;
    }

    public static final String MARKER_OFFSET = "waypointOffset";
    public static final String MARKER_FORMATTED_OFFSET = "formattedWaypointOffset";

    public static final String LABEL_PATTERN_AUTO = "?{ '{name} ({desc})' | '{name} ({cmt})' | '{name}' | '{desc}' | '{cmt}' }";
    public static final String LABEL_PATTERN_NAME = "{name}";
    public static final String LABEL_PATTERN_DESC = "{desc}";

    private final DateFormat timeFormatter = DateUtils.getGpxFormat();
    private final TemplateEngineDataProvider dataProvider;
    private final String text;

    protected final ImageIcon symbol;
    private BufferedImage redSymbol;
    public final MarkerLayer parentLayer;
    /** Absolute time of marker in seconds since epoch */
    public double time;
    /** Time offset in seconds from the gpx point from which it was derived, may be adjusted later to sync with other data, so not final */
    public double offset;

    private String cachedText;
    private int textVersion = -1;
    private CachedLatLon coor;

    private boolean erroneous;

    public Marker(LatLon ll, TemplateEngineDataProvider dataProvider, String iconName, MarkerLayer parentLayer,
            double time, double offset) {
        this(ll, dataProvider, null, iconName, parentLayer, time, offset);
    }

    public Marker(LatLon ll, String text, String iconName, MarkerLayer parentLayer, double time, double offset) {
        this(ll, null, text, iconName, parentLayer, time, offset);
    }

    private Marker(LatLon ll, TemplateEngineDataProvider dataProvider, String text, String iconName, MarkerLayer parentLayer,
            double time, double offset) {
        setCoor(ll);

        this.offset = offset;
        this.time = time;
        /* tell icon checking that we expect these names to exist */
        // /* ICON(markers/) */"Bridge"
        // /* ICON(markers/) */"Crossing"
        this.symbol = iconName != null ? ImageProvider.getIfAvailable("markers", iconName) : null;
        this.parentLayer = parentLayer;

        this.dataProvider = dataProvider;
        this.text = text;
    }

    /**
     * Convert Marker to WayPoint so it can be exported to a GPX file.
     *
     * Override in subclasses to add all necessary attributes.
     *
     * @return the corresponding WayPoint with all relevant attributes
     */
    public WayPoint convertToWayPoint() {
        WayPoint wpt = new WayPoint(getCoor());
        wpt.put(GpxConstants.PT_TIME, timeFormatter.format(new Date(Math.round(time * 1000))));
        if (text != null) {
            wpt.addExtension("text", text);
        } else if (dataProvider != null) {
            for (String key : dataProvider.getTemplateKeys()) {
                Object value = dataProvider.getTemplateValue(key, false);
                if (value != null && GpxConstants.WPT_KEYS.contains(key)) {
                    wpt.put(key, value);
                }
            }
        }
        return wpt;
    }

    /**
     * Sets the marker's coordinates.
     * @param coor The marker's coordinates (lat/lon)
     */
    public final void setCoor(LatLon coor) {
        this.coor = new CachedLatLon(coor);
    }

    /**
     * Returns the marker's coordinates.
     * @return The marker's coordinates (lat/lon)
     */
    public final LatLon getCoor() {
        return coor;
    }

    /**
     * Sets the marker's projected coordinates.
     * @param eastNorth The marker's projected coordinates (easting/northing)
     */
    public final void setEastNorth(EastNorth eastNorth) {
        this.coor = new CachedLatLon(eastNorth);
    }

    /**
     * @since 12725
     */
    @Override
    public double lon() {
        return coor == null ? Double.NaN : coor.lon();
    }

    /**
     * @since 12725
     */
    @Override
    public double lat() {
        return coor == null ? Double.NaN : coor.lat();
    }

    /**
     * Checks whether the marker display area contains the given point.
     * Markers not interested in mouse clicks may always return false.
     *
     * @param p The point to check
     * @return <code>true</code> if the marker "hotspot" contains the point.
     */
    public boolean containsPoint(Point p) {
        return false;
    }

    /**
     * Called when the mouse is clicked in the marker's hotspot. Never
     * called for markers which always return false from containsPoint.
     *
     * @param ev A dummy ActionEvent
     */
    public void actionPerformed(ActionEvent ev) {
        // Do nothing
    }

    /**
     * Paints the marker.
     * @param g graphics context
     * @param mv map view
     * @param mousePressed true if the left mouse button is pressed
     * @param showTextOrIcon true if text and icon shall be drawn
     */
    public void paint(Graphics g, MapView mv, boolean mousePressed, boolean showTextOrIcon) {
        Point screen = mv.getPoint(this);
        if (symbol != null && showTextOrIcon) {
            paintIcon(mv, g, screen.x-symbol.getIconWidth()/2, screen.y-symbol.getIconHeight()/2);
        } else {
            g.drawLine(screen.x-2, screen.y-2, screen.x+2, screen.y+2);
            g.drawLine(screen.x+2, screen.y-2, screen.x-2, screen.y+2);
        }

        String labelText = getText();
        if ((labelText != null) && showTextOrIcon) {
            g.drawString(labelText, screen.x+4, screen.y+2);
        }
    }

    protected void paintIcon(MapView mv, Graphics g, int x, int y) {
        if (!erroneous) {
            symbol.paintIcon(mv, g, x, y);
        } else {
            if (redSymbol == null) {
                int width = symbol.getIconWidth();
                int height = symbol.getIconHeight();

                redSymbol = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gbi = redSymbol.createGraphics();
                gbi.drawImage(symbol.getImage(), 0, 0, null);
                gbi.setColor(Color.RED);
                gbi.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.666f));
                gbi.fillRect(0, 0, width, height);
                gbi.dispose();
            }
            g.drawImage(redSymbol, x, y, mv);
        }
    }

    protected TemplateEntryProperty getTextTemplate() {
        return TemplateEntryProperty.forMarker(parentLayer.getName());
    }

    /**
     * Returns the Text which should be displayed, depending on chosen preference
     * @return Text of the label
     */
    public String getText() {
        if (text != null)
            return text;
        else {
            TemplateEntryProperty property = getTextTemplate();
            if (property.getUpdateCount() != textVersion) {
                TemplateEntry templateEntry = property.get();
                StringBuilder sb = new StringBuilder();
                templateEntry.appendText(sb, this);

                cachedText = sb.toString();
                textVersion = property.getUpdateCount();
            }
            return cachedText;
        }
    }

    @Override
    public Collection<String> getTemplateKeys() {
        Collection<String> result;
        if (dataProvider != null) {
            result = dataProvider.getTemplateKeys();
        } else {
            result = new ArrayList<>();
        }
        result.add(MARKER_FORMATTED_OFFSET);
        result.add(MARKER_OFFSET);
        return result;
    }

    private String formatOffset() {
        int wholeSeconds = (int) (offset + 0.5);
        if (wholeSeconds < 60)
            return Integer.toString(wholeSeconds);
        else if (wholeSeconds < 3600)
            return String.format("%d:%02d", wholeSeconds / 60, wholeSeconds % 60);
        else
            return String.format("%d:%02d:%02d", wholeSeconds / 3600, (wholeSeconds % 3600)/60, wholeSeconds % 60);
    }

    @Override
    public Object getTemplateValue(String name, boolean special) {
        if (MARKER_FORMATTED_OFFSET.equals(name))
            return formatOffset();
        else if (MARKER_OFFSET.equals(name))
            return offset;
        else if (dataProvider != null)
            return dataProvider.getTemplateValue(name, special);
        else
            return null;
    }

    @Override
    public boolean evaluateCondition(Match condition) {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines if this marker is erroneous.
     * @return {@code true} if this markers has any kind of error, {@code false} otherwise
     * @since 6299
     */
    public final boolean isErroneous() {
        return erroneous;
    }

    /**
     * Sets this marker erroneous or not.
     * @param erroneous {@code true} if this markers has any kind of error, {@code false} otherwise
     * @since 6299
     */
    public final void setErroneous(boolean erroneous) {
        this.erroneous = erroneous;
        if (!erroneous) {
            redSymbol = null;
        }
    }
}
