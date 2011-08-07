// License: GPL. Copyright 2008 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.preferences.CachedProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.ImageProvider;
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
 * will create AudioMarkers for .wav files, ImageMarkers for .png/.jpg/.jpeg
 * files, and WebMarkers for everything else. (The creation of a WebMarker will
 * fail if there's no vaild URL in the <link> tag, so it might still make sense
 * to add Makers for such waypoints at the end of the list.)
 *
 * The default implementation only looks at the value of the <link> tag inside
 * the <wpt> tag of the GPX file.
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
 * @author Frederik Ramm <frederik@remote.org>
 */
public class Marker implements TemplateEngineDataProvider {

    public static class TemplateEntryProperty extends CachedProperty<TemplateEntry> {
        // This class is a bit complicated because it supports both global and per layer settings. I've added per layer settings because
        // GPXSettingsPanel had possibility to set waypoint label but then I've realized that markers use different layer then gpx data
        // so per layer settings is useless. Anyway it's possible to specify marker layer pattern in Einstein preferences and maybe somebody
        // will make gui for it so I'm keeping it here

        private final static Map<String, TemplateEntryProperty> cache = new HashMap<String, Marker.TemplateEntryProperty>();

        // Legacy code - convert label from int to template engine expression
        private static final IntegerProperty PROP_LABEL = new IntegerProperty("draw.rawgps.layer.wpt", 0 );
        private static String getDefaultLabelPattern() {
            switch (PROP_LABEL.get()) {
            case 1:
                return LABEL_PATTERN_NAME;
            case 2:
                return LABEL_PATTERN_DESC;
            case 0:
            case 3:
                return LABEL_PATTERN_AUTO;
            default:
                return "";
            }
        }

        public static TemplateEntryProperty forMarker(String layerName) {
            String key = "draw.rawgps.layer.wpt.pattern";
            if (layerName != null) {
                key += "." + layerName;
            }
            TemplateEntryProperty result = cache.get(key);
            if (result == null) {
                String defaultValue = layerName == null?getDefaultLabelPattern():"";
                TemplateEntryProperty parent = layerName == null?null:forMarker(null);
                result = new TemplateEntryProperty(key, defaultValue, parent);
                cache.put(key, result);
            }
            return result;
        }

        public static TemplateEntryProperty forAudioMarker(String layerName) {
            String key = "draw.rawgps.layer.audiowpt.pattern";
            if (layerName != null) {
                key += "." + layerName;
            }
            TemplateEntryProperty result = cache.get(key);
            if (result == null) {
                String defaultValue = layerName == null?"?{ '{name}' | '{desc}' | '{" + Marker.MARKER_FORMATTED_OFFSET + "}' }":"";
                TemplateEntryProperty parent = layerName == null?null:forAudioMarker(null);
                result = new TemplateEntryProperty(key, defaultValue, parent);
                cache.put(key, result);
            }
            return result;
        }

        private TemplateEntryProperty parent;


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
                System.out.println(String.format("Unable to parse template engine pattern '%s' for property %s. Using default ('%s') instead",
                        s, getKey(), defaultValue));
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
     * (depending on whether they want to override default behaviour or just add new
     * stuff).
     */
    public static List<MarkerProducers> markerProducers = new LinkedList<MarkerProducers>();

    // Add one Maker specifying the default behaviour.
    static {
        Marker.markerProducers.add(new MarkerProducers() {
            @SuppressWarnings("unchecked")
            public Marker createMarker(WayPoint wpt, File relativePath, MarkerLayer parentLayer, double time, double offset) {
                String uri = null;
                // cheapest way to check whether "link" object exists and is a non-empty
                // collection of GpxLink objects...
                Collection<GpxLink> links = (Collection<GpxLink>)wpt.attr.get(GpxData.META_LINKS);
                if (links != null) {
                    for (GpxLink oneLink : links ) {
                        uri = oneLink.uri;
                        break;
                    }
                }

                URL url = null;
                if (uri != null) {
                    try {
                        url = new URL(uri);
                    } catch (MalformedURLException e) {
                        // Try a relative file:// url, if the link is not in an URL-compatible form
                        if (relativePath != null) {
                            try {
                                url = new File(relativePath.getParentFile(), uri).toURI().toURL();
                            } catch (MalformedURLException e1) {
                                System.err.println("Unable to convert uri " + uri + " to URL: "  + e1.getMessage());
                            }
                        }
                    }
                }


                if (url == null) {
                    String symbolName = wpt.getString("symbol");
                    if (symbolName == null) {
                        symbolName = wpt.getString("sym");
                    }
                    return new Marker(wpt.getCoor(), wpt, symbolName, parentLayer, time, offset);
                }
                else if (url.toString().endsWith(".wav"))
                    return new AudioMarker(wpt.getCoor(), wpt, url, parentLayer, time, offset);
                else if (url.toString().endsWith(".png") || url.toString().endsWith(".jpg") || url.toString().endsWith(".jpeg") || url.toString().endsWith(".gif"))
                    return new ImageMarker(wpt.getCoor(), url, parentLayer, time, offset);
                else
                    return new WebMarker(wpt.getCoor(), url, parentLayer, time, offset);
            }
        });
    }

    /**
     * Returns an object of class Marker or one of its subclasses
     * created from the parameters given.
     *
     * @param wpt waypoint data for marker
     * @param relativePath An path to use for constructing relative URLs or
     *        <code>null</code> for no relative URLs
     * @param offset double in seconds as the time offset of this marker from
     *        the GPX file from which it was derived (if any).
     * @return a new Marker object
     */
    public static Marker createMarker(WayPoint wpt, File relativePath, MarkerLayer parentLayer, double time, double offset) {
        for (MarkerProducers maker : Marker.markerProducers) {
            Marker marker = maker.createMarker(wpt, relativePath, parentLayer, time, offset);
            if (marker != null)
                return marker;
        }
        return null;
    }

    public static final String MARKER_OFFSET = "waypointOffset";
    public static final String MARKER_FORMATTED_OFFSET = "formattedWaypointOffset";

    public static final String LABEL_PATTERN_AUTO = "?{ '{name} - {desc}' | '{name}' | '{desc}' }";
    public static final String LABEL_PATTERN_NAME = "{name}";
    public static final String LABEL_PATTERN_DESC = "{desc}";


    private final TemplateEngineDataProvider dataProvider;
    private final String text;

    public final Icon symbol;
    public final MarkerLayer parentLayer;
    public double time; /* absolute time of marker since epoch */
    public double offset; /* time offset in seconds from the gpx point from which it was derived,
                             may be adjusted later to sync with other data, so not final */

    private String cachedText;
    private int textVersion = -1;
    private CachedLatLon coor;

    public Marker(LatLon ll, TemplateEngineDataProvider dataProvider, String iconName, MarkerLayer parentLayer, double time, double offset) {
        setCoor(ll);

        this.offset = offset;
        this.time = time;
        // /* ICON(markers/) */"Bridge"
        // /* ICON(markers/) */"Crossing"
        this.symbol = ImageProvider.getIfAvailable("markers",iconName);
        this.parentLayer = parentLayer;

        this.dataProvider = dataProvider;
        this.text = null;
    }

    public Marker(LatLon ll, String text, String iconName, MarkerLayer parentLayer, double time, double offset) {
        setCoor(ll);

        this.offset = offset;
        this.time = time;
        // /* ICON(markers/) */"Bridge"
        // /* ICON(markers/) */"Crossing"
        this.symbol = ImageProvider.getIfAvailable("markers",iconName);
        this.parentLayer = parentLayer;

        this.dataProvider = null;
        this.text = text;
    }

    public final void setCoor(LatLon coor) {
        if(this.coor == null) {
            this.coor = new CachedLatLon(coor);
        } else {
            this.coor.setCoor(coor);
        }
    }

    public final LatLon getCoor() {
        return coor;
    }

    public final void setEastNorth(EastNorth eastNorth) {
        coor.setEastNorth(eastNorth);
    }

    public final EastNorth getEastNorth() {
        return coor.getEastNorth();
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
    }


    /**
     * Paints the marker.
     * @param g graphics context
     * @param mv map view
     * @param mousePressed true if the left mouse button is pressed
     */
    public void paint(Graphics g, MapView mv, boolean mousePressed, boolean showTextOrIcon) {
        Point screen = mv.getPoint(getEastNorth());
        if (symbol != null && showTextOrIcon) {
            symbol.paintIcon(mv, g, screen.x-symbol.getIconWidth()/2, screen.y-symbol.getIconHeight()/2);
        } else {
            g.drawLine(screen.x-2, screen.y-2, screen.x+2, screen.y+2);
            g.drawLine(screen.x+2, screen.y-2, screen.x-2, screen.y+2);
        }

        String labelText = getText();
        if ((labelText != null) && showTextOrIcon) {
            g.drawString(labelText, screen.x+4, screen.y+2);
        }
    }


    protected TemplateEntryProperty getTextTemplate() {
        return TemplateEntryProperty.forMarker(parentLayer.getName());
    }

    /**
     * Returns the Text which should be displayed, depending on chosen preference
     * @return Text
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
    public List<String> getTemplateKeys() {
        List<String> result;
        if (dataProvider != null) {
            result = dataProvider.getTemplateKeys();
        } else {
            result = new ArrayList<String>();
        }
        result.add(MARKER_FORMATTED_OFFSET);
        result.add(MARKER_OFFSET);
        return result;
    }

    private String formatOffset () {
        int wholeSeconds = (int)(offset + 0.5);
        if (wholeSeconds < 60)
            return Integer.toString(wholeSeconds);
        else if (wholeSeconds < 3600)
            return String.format("%d:%02d", wholeSeconds / 60, wholeSeconds % 60);
        else
            return String.format("%d:%02d:%02d", wholeSeconds / 3600, (wholeSeconds % 3600)/60, wholeSeconds % 60);
    }

    @Override
    public Object getTemplateValue(String name) {
        if (MARKER_FORMATTED_OFFSET.equals(name))
            return formatOffset();
        else if (MARKER_OFFSET.equals(name))
            return offset;
        else if (dataProvider != null)
            return dataProvider.getTemplateValue(name);
        else
            return null;
    }

    @Override
    public boolean evaluateCondition(Match condition) {
        throw new UnsupportedOperationException();
    }
}
