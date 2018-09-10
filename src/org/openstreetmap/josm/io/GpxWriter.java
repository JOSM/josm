// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.Extensions;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IWithAttributes;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Writes GPX files from GPX data or OSM data.
 */
public class GpxWriter extends XmlWriter implements GpxConstants {

    private final DateFormat gpxFormat = DateUtils.getGpxFormat();

    /**
     * Constructs a new {@code GpxWriter}.
     * @param out The output writer
     */
    public GpxWriter(PrintWriter out) {
        super(out);
    }

    /**
     * Constructs a new {@code GpxWriter}.
     * @param out The output stream
     */
    public GpxWriter(OutputStream out) {
        super(new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))));
    }

    private GpxData data;
    private String indent = "";

    private static final int WAY_POINT = 0;
    private static final int ROUTE_POINT = 1;
    private static final int TRACK_POINT = 2;

    /**
     * Writes the given GPX data.
     * @param data The data to write
     */
    public void write(GpxData data) {
        this.data = data;
        // We write JOSM specific meta information into gpx 'extensions' elements.
        // In particular it is noted whether the gpx data is from the OSM server
        // (so the rendering of clouds of anonymous TrackPoints can be improved)
        // and some extra synchronization info for export of AudioMarkers.
        // It is checked in advance, if any extensions are used, so we know whether
        // a namespace declaration is necessary.
        boolean hasExtensions = data.fromServer;
        if (!hasExtensions) {
            for (WayPoint wpt : data.waypoints) {
                Extensions extensions = (Extensions) wpt.get(META_EXTENSIONS);
                if (extensions != null && !extensions.isEmpty()) {
                    hasExtensions = true;
                    break;
                }
            }
        }

        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.println("<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"");
        out.println((hasExtensions ? String.format("    xmlns:josm=\"%s\"%n", JOSM_EXTENSIONS_NAMESPACE_URI) : "") +
                    "    xmlns:xsi=\""+XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI+"\"");
        out.println("    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">");
        indent = "  ";
        writeMetaData();
        writeWayPoints();
        writeRoutes();
        writeTracks();
        out.print("</gpx>");
        out.flush();
    }

    private void writeAttr(IWithAttributes obj, List<String> keys) {
        for (String key : keys) {
            if (META_LINKS.equals(key)) {
                Collection<GpxLink> lValue = obj.<GpxLink>getCollection(key);
                if (lValue != null) {
                    for (GpxLink link : lValue) {
                        gpxLink(link);
                    }
                }
            } else if (META_EXTENSIONS.equals(key)) {
                Extensions extensions = (Extensions) obj.get(key);
                if (extensions != null) {
                    gpxExtensions(extensions);
                }
            } else {
                String value = obj.getString(key);
                if (value != null) {
                    simpleTag(key, value);
                } else {
                    Object val = obj.get(key);
                    if (val instanceof Date) {
                        simpleTag(key, gpxFormat.format(val));
                    } else if (val instanceof Number) {
                        simpleTag(key, val.toString());
                    } else if (val != null) {
                        Logging.warn("GPX attribute '"+key+"' not managed: " + val);
                    }
                }
            }
        }
    }

    private void writeMetaData() {
        Map<String, Object> attr = data.attr;
        openln("metadata");

        // write the description
        if (attr.containsKey(META_DESC)) {
            simpleTag("desc", data.getString(META_DESC));
        }

        // write the author details
        if (attr.containsKey(META_AUTHOR_NAME)
                || attr.containsKey(META_AUTHOR_EMAIL)) {
            openln("author");
            // write the name
            simpleTag("name", data.getString(META_AUTHOR_NAME));
            // write the email address
            if (attr.containsKey(META_AUTHOR_EMAIL)) {
                String[] tmp = data.getString(META_AUTHOR_EMAIL).split("@");
                if (tmp.length == 2) {
                    inline("email", "id=\"" + tmp[0] + "\" domain=\""+tmp[1]+'\"');
                }
            }
            // write the author link
            gpxLink((GpxLink) data.get(META_AUTHOR_LINK));
            closeln("author");
        }

        // write the copyright details
        if (attr.containsKey(META_COPYRIGHT_LICENSE)
                || attr.containsKey(META_COPYRIGHT_YEAR)) {
            openAtt("copyright", "author=\""+ data.get(META_COPYRIGHT_AUTHOR) +'\"');
            if (attr.containsKey(META_COPYRIGHT_YEAR)) {
                simpleTag("year", (String) data.get(META_COPYRIGHT_YEAR));
            }
            if (attr.containsKey(META_COPYRIGHT_LICENSE)) {
                simpleTag("license", encode((String) data.get(META_COPYRIGHT_LICENSE)));
            }
            closeln("copyright");
        }

        // write links
        if (attr.containsKey(META_LINKS)) {
            for (GpxLink link : data.<GpxLink>getCollection(META_LINKS)) {
                gpxLink(link);
            }
        }

        // write keywords
        if (attr.containsKey(META_KEYWORDS)) {
            simpleTag("keywords", data.getString(META_KEYWORDS));
        }

        Bounds bounds = data.recalculateBounds();
        if (bounds != null) {
            String b = "minlat=\"" + bounds.getMinLat() + "\" minlon=\"" + bounds.getMinLon() +
            "\" maxlat=\"" + bounds.getMaxLat() + "\" maxlon=\"" + bounds.getMaxLon() + '\"';
            inline("bounds", b);
        }

        if (data.fromServer) {
            openln("extensions");
            simpleTag("josm:from-server", "true");
            closeln("extensions");
        }

        closeln("metadata");
    }

    private void writeWayPoints() {
        for (WayPoint pnt : data.getWaypoints()) {
            wayPoint(pnt, WAY_POINT);
        }
    }

    private void writeRoutes() {
        for (GpxRoute rte : data.getRoutes()) {
            openln("rte");
            writeAttr(rte, RTE_TRK_KEYS);
            for (WayPoint pnt : rte.routePoints) {
                wayPoint(pnt, ROUTE_POINT);
            }
            closeln("rte");
        }
    }

    private void writeTracks() {
        for (GpxTrack trk : data.getTracks()) {
            openln("trk");
            writeAttr(trk, RTE_TRK_KEYS);
            for (GpxTrackSegment seg : trk.getSegments()) {
                openln("trkseg");
                for (WayPoint pnt : seg.getWayPoints()) {
                    wayPoint(pnt, TRACK_POINT);
                }
                closeln("trkseg");
            }
            closeln("trk");
        }
    }

    private void openln(String tag) {
        open(tag);
        out.println();
    }

    private void open(String tag) {
        out.print(indent + '<' + tag + '>');
        indent += "  ";
    }

    private void openAtt(String tag, String attributes) {
        out.println(indent + '<' + tag + ' ' + attributes + '>');
        indent += "  ";
    }

    private void inline(String tag, String attributes) {
        out.println(indent + '<' + tag + ' ' + attributes + "/>");
    }

    private void close(String tag) {
        indent = indent.substring(2);
        out.print(indent + "</" + tag + '>');
    }

    private void closeln(String tag) {
        close(tag);
        out.println();
    }

    /**
     * if content not null, open tag, write encoded content, and close tag
     * else do nothing.
     * @param tag GPX tag
     * @param content content
     */
    private void simpleTag(String tag, String content) {
        if (content != null && !content.isEmpty()) {
            open(tag);
            out.print(encode(content));
            out.println("</" + tag + '>');
            indent = indent.substring(2);
        }
    }

    /**
     * output link
     * @param link link
     */
    private void gpxLink(GpxLink link) {
        if (link != null) {
            openAtt("link", "href=\"" + link.uri + '\"');
            simpleTag("text", link.text);
            simpleTag("type", link.type);
            closeln("link");
        }
    }

    /**
     * output a point
     * @param pnt waypoint
     * @param mode {@code WAY_POINT} for {@code wpt}, {@code ROUTE_POINT} for {@code rtept}, {@code TRACK_POINT} for {@code trkpt}
     */
    private void wayPoint(WayPoint pnt, int mode) {
        String type;
        switch(mode) {
        case WAY_POINT:
            type = "wpt";
            break;
        case ROUTE_POINT:
            type = "rtept";
            break;
        case TRACK_POINT:
            type = "trkpt";
            break;
        default:
            throw new JosmRuntimeException(tr("Unknown mode {0}.", mode));
        }
        if (pnt != null) {
            LatLon c = pnt.getCoor();
            String coordAttr = "lat=\"" + c.lat() + "\" lon=\"" + c.lon() + '\"';
            if (pnt.attr.isEmpty()) {
                inline(type, coordAttr);
            } else {
                openAtt(type, coordAttr);
                writeAttr(pnt, WPT_KEYS);
                closeln(type);
            }
        }
    }

    private void gpxExtensions(Extensions extensions) {
        if (extensions != null && !extensions.isEmpty()) {
            openln("extensions");
            for (Entry<String, String> e : extensions.entrySet()) {
                simpleTag("josm:" + e.getKey(), e.getValue());
            }
            closeln("extensions");
        }
    }
}
