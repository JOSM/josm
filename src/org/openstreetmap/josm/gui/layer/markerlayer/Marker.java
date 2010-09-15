// License: GPL. Copyright 2008 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.Icon;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.ImageProvider;

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
public class Marker implements ActionListener {
    public final String text;
    public final Map<String,String> textMap = new HashMap<String,String>();
    public final Icon symbol;
    public final MarkerLayer parentLayer;
    public double time; /* absolute time of marker since epoch */
    public double offset; /* time offset in seconds from the gpx point from which it was derived,
                             may be adjusted later to sync with other data, so not final */

    private CachedLatLon coor;

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
     * Plugins can add their Marker creation stuff at the bottom or top of this list
     * (depending on whether they want to override default behaviour or just add new
     * stuff).
     */
    public static LinkedList<MarkerProducers> markerProducers = new LinkedList<MarkerProducers>();

    private static final IntegerProperty PROP_LABEL = new IntegerProperty("draw.rawgps.layer.wpt", 0 );
    private static final String[] labelAttributes = new String[] {"name", "desc"};

    // Add one Maker specifying the default behaviour.
    static {
        Marker.markerProducers.add(new MarkerProducers() {
            @SuppressWarnings("unchecked")
            public Marker createMarker(WayPoint wpt, File relativePath, MarkerLayer parentLayer, double time, double offset) {
                String uri = null;
                // cheapest way to check whether "link" object exists and is a non-empty
                // collection of GpxLink objects...
                try {
                    for (GpxLink oneLink : (Collection<GpxLink>) wpt.attr.get(GpxData.META_LINKS)) {
                        uri = oneLink.uri;
                        break;
                    }
                } catch (Exception ex) {}

                // Try a relative file:// url, if the link is not in an URL-compatible form
                if (relativePath != null && uri != null && !isWellFormedAddress(uri)) {
                    uri = new File(relativePath.getParentFile(), uri).toURI().toString();
                }

                Map<String,String> nameDesc = new HashMap<String,String>();
                for(String attribute : labelAttributes) {
                    if (wpt.attr.containsKey(attribute)) {
                        nameDesc.put(attribute, wpt.getString(attribute));
                    }
                }

                if (uri == null) {
                    String symbolName = wpt.getString("symbol");
                    if (symbolName == null) {
                        symbolName = wpt.getString("sym");
                    }
                    return new Marker(wpt.getCoor(), nameDesc, symbolName, parentLayer, time, offset);
                }
                else if (uri.endsWith(".wav"))
                    return AudioMarker.create(wpt.getCoor(), getText(nameDesc), uri, parentLayer, time, offset);
                else if (uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".gif"))
                    return ImageMarker.create(wpt.getCoor(), uri, parentLayer, time, offset);
                else
                    return WebMarker.create(wpt.getCoor(), uri, parentLayer, time, offset);
            }

            private boolean isWellFormedAddress(String link) {
                try {
                    new URL(link);
                    return true;
                } catch (MalformedURLException x) {
                    return false;
                }
            }
        });
    }

    public Marker(LatLon ll, String text, String iconName, MarkerLayer parentLayer, double time, double offset) {
        setCoor(ll);
        if (text == null || text.length() == 0) {
            this.text = null;
        }
        else {
            this.text = text;
        }
        this.offset = offset;
        this.time = time;
        this.symbol = ImageProvider.getIfAvailable("markers",iconName);
        this.parentLayer = parentLayer;
    }

    public Marker(LatLon ll, Map<String,String> textMap, String iconName, MarkerLayer parentLayer, double time, double offset) {
        setCoor(ll);
        if (textMap != null) {
            this.textMap.clear();
            this.textMap.putAll(textMap);
        }

        this.text = null;
        this.offset = offset;
        this.time = time;
        this.symbol = ImageProvider.getIfAvailable("markers",iconName);
        this.parentLayer = parentLayer;
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

    /**
     * Returns an AudioMarker derived from this Marker and the provided uri
     * Subclasses of specific marker types override this to return null as they can't
     * be turned into AudioMarkers. This includes AudioMarkers themselves, as they
     * already have audio.
     *
     * @param uri uri of wave file
     * @return AudioMarker
     */

    public AudioMarker audioMarkerFromMarker(String uri) {
        AudioMarker audioMarker = AudioMarker.create(getCoor(), this.getText(), uri, this.parentLayer, this.time, this.offset);
        return audioMarker;
    }

    /**
     * Returns the Text which should be displayed, depending on chosen preference
     * @return Text
     */
    public String getText() {
        if (this.text != null ) {
            return this.text;
        }
        else {
            return getText(this.textMap);
        }
    }

    /**
     * Returns the Text which should be displayed, depending on chosen preference.
     * The possible attributes are read from textMap.
     *
     * @param textMap A map with available texts/attributes
     * @return Text
     */
    private static String getText(Map<String,String> textMap) {
        String text = "";

        if (textMap != null && !textMap.isEmpty()) {
            switch(PROP_LABEL.get())
            {
                // name
                case 1:
                {
                    if (textMap.containsKey("name")) {
                        text = textMap.get("name");
                    }
                    break;
                }

                // desc
                case 2:
                {
                    if (textMap.containsKey("desc")) {
                        text = textMap.get("desc");
                    }
                    break;
                }

                // auto
                case 0:
                // both
                case 3:
                {
                    if (textMap.containsKey("name")) {
                        text = textMap.get("name");

                        if (textMap.containsKey("desc")) {
                            if (PROP_LABEL.get() != 0 || !text.equals(textMap.get("desc"))) {
                                text += " - " + textMap.get("desc");
                            }
                        }
                    }
                    else if (textMap.containsKey("desc")) {
                        text = textMap.get("desc");
                    }
                    break;
                }

                // none
                case 4:
                default:
                {
                    text = "";
                    break;
                }
            }
        }

        return text;
    }
}
