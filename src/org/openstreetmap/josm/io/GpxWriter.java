// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxData.XMLNamespace;
import org.openstreetmap.josm.data.gpx.GpxExtension;
import org.openstreetmap.josm.data.gpx.GpxExtensionCollection;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IWithAttributes;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Writes GPX files from GPX data or OSM data.
 */
public class GpxWriter extends XmlWriter implements GpxConstants {

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
    private List<String> validprefixes;

    private static final int WAY_POINT = 0;
    private static final int ROUTE_POINT = 1;
    private static final int TRACK_POINT = 2;

    /**
     * Writes the given GPX data.
     * @param data The data to write
     */
    public void write(GpxData data) {
        write(data, ColorFormat.GPXD, true);
    }

    /**
     * Writes the given GPX data.
     *
     * @param data The data to write
     * @param colorFormat determines if colors are saved and which extension is to be used
     * @param savePrefs whether layer specific preferences are saved
     */
    public void write(GpxData data, ColorFormat colorFormat, boolean savePrefs) {
        this.data = data;

        //Prepare extensions for writing
        data.beginUpdate();
        data.getTracks().stream()
        .filter(GpxTrack.class::isInstance).map(GpxTrack.class::cast)
        .forEach(trk -> trk.convertColor(colorFormat));
        data.getExtensions().removeAllWithPrefix("josm");
        if (data.fromServer) {
            data.getExtensions().add("josm", "from-server", "true");
        }
        if (savePrefs && !data.getLayerPrefs().isEmpty()) {
            GpxExtensionCollection layerExts = data.getExtensions().add("josm", "layerPreferences").getExtensions();
            data.getLayerPrefs().entrySet()
            .stream()
            .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
            .forEach(entry -> {
                GpxExtension e = layerExts.add("josm", "entry");
                e.put("key", entry.getKey());
                e.put("value", entry.getValue());
            });
        }
        data.endUpdate();

        Collection<IWithAttributes> all = new ArrayList<>();

        all.add(data);
        all.addAll(data.getWaypoints());
        all.addAll(data.getRoutes());
        all.addAll(data.getTracks());
        all.addAll(data.getTrackSegmentsStream().collect(Collectors.toList()));

        List<XMLNamespace> namespaces = all
                .stream()
                .flatMap(w -> w.getExtensions().getPrefixesStream())
                .distinct()
                .map(p -> data.getNamespaces()
                        .stream()
                        .filter(s -> s.getPrefix().equals(p))
                        .findAny()
                        .orElse(GpxExtension.findNamespace(p)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        validprefixes = namespaces.stream().map(n -> n.getPrefix()).collect(Collectors.toList());

        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.println("<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"");

        StringBuilder schemaLocations = new StringBuilder("http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");

        for (XMLNamespace n : namespaces) {
            if (n.getURI() != null && n.getPrefix() != null && !n.getPrefix().isEmpty()) {
                out.println(String.format("    xmlns:%s=\"%s\"", n.getPrefix(), n.getURI()));
                if (n.getLocation() != null) {
                    schemaLocations.append(' ').append(n.getURI()).append(' ').append(n.getLocation());
                }
            }
        }

        out.println("    xmlns:xsi=\""+XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI+"\"");
        out.println(String.format("    xsi:schemaLocation=\"%s\">", schemaLocations));
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
            } else {
                String value = obj.getString(key);
                if (value != null) {
                    simpleTag(key, value);
                } else {
                    Object val = obj.get(key);
                    if (val instanceof Date) {
                        simpleTag(key, DateUtils.fromDate((Date) val));
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
                    inline("email", "id=\"" + encode(tmp[0]) + "\" domain=\"" + encode(tmp[1]) +'\"');
                }
            }
            // write the author link
            gpxLink((GpxLink) data.get(META_AUTHOR_LINK));
            closeln("author");
        }

        // write the copyright details
        if (attr.containsKey(META_COPYRIGHT_LICENSE)
                || attr.containsKey(META_COPYRIGHT_YEAR)) {
            openln("copyright", "author=\""+ encode(data.get(META_COPYRIGHT_AUTHOR).toString()) +'\"');
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

        gpxExtensions(data.getExtensions());
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
            gpxExtensions(rte.getExtensions());
            for (WayPoint pnt : rte.routePoints) {
                wayPoint(pnt, ROUTE_POINT);
            }
            closeln("rte");
        }
    }

    private void writeTracks() {
        for (IGpxTrack trk : data.getTracks()) {
            openln("trk");
            writeAttr(trk, RTE_TRK_KEYS);
            gpxExtensions(trk.getExtensions());
            for (IGpxTrackSegment seg : trk.getSegments()) {
                openln("trkseg");
                gpxExtensions(seg.getExtensions());
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

    private void openln(String tag, String attributes) {
        open(tag, attributes);
        out.println();
    }

    private void open(String tag) {
        out.print(indent + '<' + tag + '>');
        indent += "  ";
    }

    private void open(String tag, String attributes) {
        out.print(indent + '<' + tag + (attributes.isEmpty() ? "" : ' ') + attributes + '>');
        indent += "  ";
    }

    private void inline(String tag, String attributes) {
        out.println(indent + '<' + tag + (attributes.isEmpty() ? "" : ' ') + attributes + "/>");
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

    private void simpleTag(String tag, String content, String attributes) {
        if (content != null && !content.isEmpty()) {
            open(tag, attributes);
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
            openln("link", "href=\"" + encode(link.uri) + '\"');
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
            if (pnt.attr.isEmpty() && pnt.getExtensions().isEmpty()) {
                inline(type, coordAttr);
            } else {
                openln(type, coordAttr);
                writeAttr(pnt, WPT_KEYS);
                gpxExtensions(pnt.getExtensions());
                closeln(type);
            }
        }
    }

    private void gpxExtensions(GpxExtensionCollection allExtensions) {
        if (allExtensions.isVisible()) {
            openln("extensions");
            writeExtension(allExtensions);
            closeln("extensions");
        }
    }

    private void writeExtension(List<GpxExtension> extensions) {
        for (GpxExtension e : extensions) {
            if (validprefixes.contains(e.getPrefix()) && e.isVisible()) {
                // this might lead to loss of an unknown extension *after* the file was saved as .osm,
                // but otherwise the file is invalid and can't even be parsed by SAX anymore
                String k = (e.getPrefix().isEmpty() ? "" : e.getPrefix() + ":") + e.getKey();
                String attr = e.getAttributes().entrySet().stream()
                        .map(a -> encode(a.getKey()) + "=\"" + encode(a.getValue().toString()) + "\"")
                        .sorted()
                        .collect(Collectors.joining(" "));
                if (e.getValue() == null && e.getExtensions().isEmpty()) {
                    inline(k, attr);
                } else if (e.getExtensions().isEmpty()) {
                    simpleTag(k, e.getValue(), attr);
                } else {
                    openln(k, attr);
                    if (e.getValue() != null) {
                        out.print(encode(e.getValue()));
                    }
                    writeExtension(e.getExtensions());
                    closeln(k);
                }
            }
        }
    }
}
