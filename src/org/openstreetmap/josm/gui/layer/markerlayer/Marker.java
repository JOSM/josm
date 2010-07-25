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
import java.util.LinkedList;

import javax.swing.Icon;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.preferences.StringProperty;
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

    private static final StringProperty PROP_NAME_DESC = new StringProperty( "draw.gpx.layer.wpt", "nameordesc" );

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

                String name_desc = "";
                if (PROP_NAME_DESC.get() == null || "nameordesc".equals(PROP_NAME_DESC.get()))
                {
                    if (wpt.attr.containsKey("name")) {
                        name_desc = wpt.getString("name");
                    } else if (wpt.attr.containsKey("desc")) {
                        name_desc = wpt.getString("desc");
                    }
                } else if ("name".equals(PROP_NAME_DESC.get())) {
                    if (wpt.attr.containsKey("name")) {
                        name_desc = wpt.getString("name");
                    }
                }
                else if ("desc".equals(PROP_NAME_DESC.get())) {
                    if (wpt.attr.containsKey("desc")) {
                        name_desc = wpt.getString("desc");
                    }
                }
                else if ("both".equals(PROP_NAME_DESC.get()) ) {
                    if (wpt.attr.containsKey("name")) {
                        name_desc = wpt.getString("name");

                        if (wpt.attr.containsKey("desc")) {
                            name_desc += " (" + wpt.getString("desc") + ")" ;
                        }
                    } else if (wpt.attr.containsKey("desc")) {
                        name_desc = wpt.getString("desc");
                    }
                }

                if (uri == null) {
                    String symbolName = wpt.getString("symbol");
                    if (symbolName == null) {
                        symbolName = wpt.getString("sym");
                    }
                    return new Marker(wpt.getCoor(), name_desc, symbolName, parentLayer, time, offset);
                }
                else if (uri.endsWith(".wav"))
                    return AudioMarker.create(wpt.getCoor(), name_desc, uri, parentLayer, time, offset);
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
        this.text = text;
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

        if ((text != null) && showTextOrIcon) {
            g.drawString(text, screen.x+4, screen.y+2);
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
        AudioMarker audioMarker = AudioMarker.create(getCoor(), this.text, uri, this.parentLayer, this.time, this.offset);
        return audioMarker;
    }
}
