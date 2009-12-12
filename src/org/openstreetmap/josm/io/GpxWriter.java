// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Writes GPX files from GPX data or OSM data.
 */
public class GpxWriter extends XmlWriter {

    public GpxWriter(PrintWriter out) {
        super(out);
    }

    public GpxWriter(OutputStream out) throws UnsupportedEncodingException {
        super(new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))));
    }

    public GpxWriter() {
        super(null);
        //sorry for this one here, this will be cleaned up once the new scheme works
    }

    private GpxData data;
    private String indent = "";

    private final static int WAY_POINT = 0;
    private final static int ROUTE_POINT = 1;
    private final static int TRACK_POINT = 2;

    public void write(GpxData data) {
        this.data = data;
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.println("<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
        "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">");
        indent = "  ";
        writeMetaData();
        writeWayPoints();
        writeRoutes();
        writeTracks();
        out.print("</gpx>");
        out.flush();
    }

    @SuppressWarnings("unchecked")
    private void writeAttr(Map<String, Object> attr) {
        // FIXME this loop is evil, because it does not assure the
        // correct element order specified by the xml schema.
        // for now it works, but future extension could get very complex and unmaintainable
        for (Map.Entry<String, Object> ent : attr.entrySet()) {
            String k = ent.getKey();
            if (k.equals(GpxData.META_LINKS)) {
                for (GpxLink link : (Collection<GpxLink>) ent.getValue()) {
                    gpxLink(link);
                }
            } else {
                simpleTag(k, ent.getValue().toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeMetaData() {
        Map<String, Object> attr = data.attr;
        openln("metadata");

        // write the description
        if (attr.containsKey(GpxData.META_DESC)) {
            simpleTag("desc", (String)attr.get(GpxData.META_DESC));
        }

        // write the author details
        if (attr.containsKey(GpxData.META_AUTHOR_NAME)
                || attr.containsKey(GpxData.META_AUTHOR_EMAIL)) {
            openln("author");
            // write the name
            simpleTag("name", (String) attr.get(GpxData.META_AUTHOR_NAME));
            // write the email address
            if(attr.containsKey(GpxData.META_AUTHOR_EMAIL)) {
                String[] tmp = ((String)attr.get(GpxData.META_AUTHOR_EMAIL)).split("@");
                if(tmp.length == 2) {
                    inline("email", "id=\"" + tmp[0] + "\" domain=\""+tmp[1]+"\"");
                }
            }
            // write the author link
            gpxLink((GpxLink) attr.get(GpxData.META_AUTHOR_LINK));
            closeln("author");
        }

        // write the copyright details
        if(attr.containsKey(GpxData.META_COPYRIGHT_LICENSE)
                || attr.containsKey(GpxData.META_COPYRIGHT_YEAR)) {
            openAtt("copyright", "author=\""+ attr.get(GpxData.META_COPYRIGHT_AUTHOR) +"\"");
            if(attr.containsKey(GpxData.META_COPYRIGHT_YEAR)) {
                simpleTag("year", (String) attr.get(GpxData.META_COPYRIGHT_YEAR));
            }
            if(attr.containsKey(GpxData.META_COPYRIGHT_LICENSE)) {
                simpleTag("license", encode((String) attr.get(GpxData.META_COPYRIGHT_LICENSE)));
            }
            closeln("copyright");
        }

        // write links
        if(attr.containsKey(GpxData.META_LINKS)) {
            for (GpxLink link : (Collection<GpxLink>) attr.get(GpxData.META_LINKS)) {
                gpxLink(link);
            }
        }

        // write keywords
        if (attr.containsKey(GpxData.META_KEYWORDS)) {
            simpleTag("keywords", (String)attr.get(GpxData.META_KEYWORDS));
        }

        Bounds bounds = data.recalculateBounds();
        if(bounds != null)
        {
            String b = "minlat=\"" + bounds.getMin().lat() + "\" minlon=\"" + bounds.getMin().lon() +
            "\" maxlat=\"" + bounds.getMax().lat() + "\" maxlon=\"" + bounds.getMax().lon() + "\"" ;
            inline("bounds", b);
        }

        closeln("metadata");
    }

    private void writeWayPoints() {
        for (WayPoint pnt : data.waypoints) {
            wayPoint(pnt, WAY_POINT);
        }
    }

    private void writeRoutes() {
        for (GpxRoute rte : data.routes) {
            openln("rte");
            writeAttr(rte.attr);
            for (WayPoint pnt : rte.routePoints) {
                wayPoint(pnt, ROUTE_POINT);
            }
            closeln("rte");
        }
    }

    private void writeTracks() {
        for (GpxTrack trk : data.tracks) {
            open("trk");
            writeAttr(trk.attr);
            for (Collection<WayPoint> seg : trk.trackSegs) {
                openln("trkseg");
                for (WayPoint pnt : seg) {
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
        out.print(indent + "<" + tag + ">");
        indent += "  ";
    }

    private void openAtt(String tag, String attributes) {
        out.println(indent + "<" + tag + " " + attributes + ">");
        indent += "  ";
    }

    private void inline(String tag, String attributes) {
        out.println(indent + "<" + tag + " " + attributes + " />");
    }

    private void close(String tag) {
        indent = indent.substring(2);
        out.print(indent + "</" + tag + ">");
    }

    private void closeln(String tag) {
        close(tag);
        out.println();
    }

    /**
     * if content not null, open tag, write encoded content, and close tag
     * else do nothing.
     */
    private void simpleTag(String tag, String content) {
        if (content != null && content.length() > 0) {
            open(tag);
            out.print(encode(content));
            out.println("</" + tag + ">");
            indent = indent.substring(2);
        }
    }

    /**
     * output link
     */
    private void gpxLink(GpxLink link) {
        if (link != null) {
            openAtt("link", "href=\"" + link.uri + "\"");
            simpleTag("text", link.text);
            simpleTag("type", link.type);
            closeln("link");
        }
    }

    /**
     * output a point
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
            throw new RuntimeException(tr("Unknown mode {0}.", mode));
        }
        if (pnt != null) {
            LatLon c = pnt.getCoor();
            openAtt(type, "lat=\"" + c.lat() + "\" lon=\"" + c.lon() + "\"");
            writeAttr(pnt.attr);
            closeln(type);
        }
    }
}
