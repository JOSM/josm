// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.data.gpx.GpxConstants.META_AUTHOR_EMAIL;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_AUTHOR_LINK;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_AUTHOR_NAME;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_BOUNDS;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_COPYRIGHT_AUTHOR;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_COPYRIGHT_LICENSE;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_COPYRIGHT_YEAR;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_DESC;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_KEYWORDS;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_LINKS;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_NAME;
import static org.openstreetmap.josm.data.gpx.GpxConstants.META_TIME;
import static org.openstreetmap.josm.data.gpx.GpxConstants.PT_TIME;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxExtensionCollection;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A parser for gpx files
 */
class GpxParser extends DefaultHandler {
    private enum State {
        INIT,
        GPX,
        METADATA,
        WPT,
        RTE,
        TRK,
        EXT,
        AUTHOR,
        LINK,
        TRKSEG,
        COPYRIGHT
    }

    private String version;
    private GpxData data;
    private Collection<IGpxTrackSegment> currentTrack;
    private Map<String, Object> currentTrackAttr;
    private Collection<WayPoint> currentTrackSeg;
    private GpxRoute currentRoute;
    private WayPoint currentWayPoint;

    private State currentState = State.INIT;

    private GpxLink currentLink;
    private GpxExtensionCollection currentExtensionCollection;
    private GpxExtensionCollection currentTrackExtensionCollection;
    private final Stack<State> states = new Stack<>();
    private final Stack<String[]> elements = new Stack<>();

    private StringBuilder accumulator = new StringBuilder();

    private boolean nokiaSportsTrackerBug;

    @Override
    public void startDocument() {
        accumulator = new StringBuilder();
        data = new GpxData(true);
        currentExtensionCollection = new GpxExtensionCollection();
        currentTrackExtensionCollection = new GpxExtensionCollection();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
        data.getNamespaces().add(new GpxData.XMLNamespace(prefix, uri));
    }

    /**
     * Convert the specified key's value to a number
     * @param attributes The attributes to get the value from
     * @param key The key to use
     * @return A valid double, or {@link Double#NaN}
     */
    private static double parseCoordinates(Attributes attributes, String key) {
        String val = attributes.getValue(key);
        if (val != null) {
            return parseCoordinates(val);
        } else {
            // Some software do not respect GPX schema and use "minLat" / "minLon" instead of "minlat" / "minlon"
            return parseCoordinates(attributes.getValue(key.replaceFirst("l", "L")));
        }
    }

    /**
     * Convert a string coordinate to a double
     * @param s The string to convert to double
     * @return A valid double, or {@link Double#NaN}
     */
    private static double parseCoordinates(String s) {
        if (s != null) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                Logging.trace(ex);
            }
        }
        return Double.NaN;
    }

    /**
     * Convert coordinates in attributes to a {@link LatLon} object
     * @param attributes The attributes to parse
     * @return The {@link LatLon}, warning: it may be invalid, use {@link LatLon#isValid()}
     */
    private static LatLon parseLatLon(Attributes attributes) {
        return new LatLon(
                parseCoordinates(attributes, "lat"),
                parseCoordinates(attributes, "lon"));
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        elements.push(new String[] {namespaceURI, localName, qName});
        switch(currentState) {
            case INIT:
                startElementInit(attributes);
                break;
            case GPX:
                startElementGpx(localName, attributes);
                break;
            case METADATA:
                startElementMetadata(localName, attributes);
                break;
            case AUTHOR:
                startElementAuthor(localName, attributes);
                break;
            case TRK:
                startElementTrk(localName, attributes);
                break;
            case TRKSEG:
                startElementTrkSeg(localName, attributes);
                break;
            case WPT:
                startElementWpt(localName, attributes);
                break;
            case RTE:
                startElementRte(localName, attributes);
                break;
            case EXT:
                startElementExt(namespaceURI, qName, attributes);
                break;
            default: // Do nothing
        }
        accumulator.setLength(0);
    }

    /**
     * Start the root element
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     */
    private void startElementInit(Attributes attributes) {
        states.push(currentState);
        currentState = State.GPX;
        data.creator = attributes.getValue("creator");
        version = attributes.getValue("version");
        if (version != null && version.startsWith("1.0")) {
            version = "1.0";
        } else if (!"1.1".equals(version)) {
            // unknown version, assume 1.1
            version = "1.1";
        }
        String schemaLocation = attributes.getValue(GpxConstants.XML_URI_XSD, "schemaLocation");
        if (schemaLocation != null) {
            String[] schemaLocations = schemaLocation.split(" ", -1);
            for (int i = 0; i < schemaLocations.length - 1; i += 2) {
                final String schemaURI = schemaLocations[i];
                final String schemaXSD = schemaLocations[i + 1];
                data.getNamespaces().stream().filter(xml -> xml.getURI().equals(schemaURI))
                        .forEach(xml -> xml.setLocation(schemaXSD));
            }
        }
    }

    /**
     * Start the root gpx element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     */
    private void startElementGpx(String localName, Attributes attributes) {
        switch (localName) {
            case "metadata":
                states.push(currentState);
                currentState = State.METADATA;
                break;
            case "wpt":
                states.push(currentState);
                currentState = State.WPT;
                currentWayPoint = new WayPoint(parseLatLon(attributes));
                break;
            case "rte":
                states.push(currentState);
                currentState = State.RTE;
                currentRoute = new GpxRoute();
                break;
            case "trk":
                states.push(currentState);
                currentState = State.TRK;
                currentTrack = new ArrayList<>();
                currentTrackAttr = new HashMap<>();
                break;
            case "extensions":
                states.push(currentState);
                currentState = State.EXT;
                break;
            case "gpx":
                if (attributes.getValue("creator") != null && attributes.getValue("creator").startsWith("Nokia Sports Tracker")) {
                    nokiaSportsTrackerBug = true;
                }
                break;
            default: // Do nothing
        }
    }

    /**
     * Start a metadata element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     * @see #endElementMetadata(String)
     */
    private void startElementMetadata(String localName, Attributes attributes) {
        switch (localName) {
            case "author":
                states.push(currentState);
                currentState = State.AUTHOR;
                break;
            case "extensions":
                states.push(currentState);
                currentState = State.EXT;
                break;
            case "copyright":
                states.push(currentState);
                currentState = State.COPYRIGHT;
                data.put(META_COPYRIGHT_AUTHOR, attributes.getValue("author"));
                break;
            case "link":
                states.push(currentState);
                currentState = State.LINK;
                currentLink = new GpxLink(attributes.getValue("href"));
                break;
            case "bounds":
                data.put(META_BOUNDS, new Bounds(
                        parseCoordinates(attributes, "minlat"),
                        parseCoordinates(attributes, "minlon"),
                        parseCoordinates(attributes, "maxlat"),
                        parseCoordinates(attributes, "maxlon")));
                break;
            default: // Do nothing
        }
    }

    /**
     * Start an author element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     * @see #endElementAuthor(String)
     */
    private void startElementAuthor(String localName, Attributes attributes) {
        switch (localName) {
            case "link":
                states.push(currentState);
                currentState = State.LINK;
                currentLink = new GpxLink(attributes.getValue("href"));
                break;
            case "email":
                data.put(META_AUTHOR_EMAIL, attributes.getValue("id") + '@' + attributes.getValue("domain"));
                break;
            default: // Do nothing
        }
    }

    /**
     * Start a trk element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     * @see #endElementTrk(String)
     */
    private void startElementTrk(String localName, Attributes attributes) {
        switch (localName) {
            case "trkseg":
                states.push(currentState);
                currentState = State.TRKSEG;
                currentTrackSeg = new ArrayList<>();
                break;
            case "link":
                states.push(currentState);
                currentState = State.LINK;
                currentLink = new GpxLink(attributes.getValue("href"));
                break;
            case "extensions":
                states.push(currentState);
                currentState = State.EXT;
                break;
            default: // Do nothing
        }
    }

    /**
     * Start a trkseg element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     * @see #endElementTrkseg(String)
     */
    private void startElementTrkSeg(String localName, Attributes attributes) {
        switch (localName) {
            case "trkpt":
                states.push(currentState);
                currentState = State.WPT;
                currentWayPoint = new WayPoint(parseLatLon(attributes));
                break;
            case "extensions":
                states.push(currentState);
                currentState = State.EXT;
                break;
            default: // Do nothing
        }
    }

    /**
     * Start the wpt element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     * @see #endElementWpt(String)
     */
    private void startElementWpt(String localName, Attributes attributes) {
        switch (localName) {
            case "link":
                states.push(currentState);
                currentState = State.LINK;
                currentLink = new GpxLink(attributes.getValue("href"));
                break;
            case "extensions":
                states.push(currentState);
                currentState = State.EXT;
                break;
            default: // Do nothing
        }
    }

    /**
     * Start the rte element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     */
    private void startElementRte(String localName, Attributes attributes) {
        switch (localName) {
            case "link":
                states.push(currentState);
                currentState = State.LINK;
                currentLink = new GpxLink(attributes.getValue("href"));
                break;
            case "rtept":
                states.push(currentState);
                currentState = State.WPT;
                currentWayPoint = new WayPoint(parseLatLon(attributes));
                break;
            case "extensions":
                states.push(currentState);
                currentState = State.EXT;
                break;
            default: // Do nothing
        }
    }

    /**
     * Start an ext element
     * @param namespaceURI The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace
     *                     processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
     */
    private void startElementExt(String namespaceURI, String qName, Attributes attributes) {
        if (states.lastElement() == State.TRK) {
            currentTrackExtensionCollection.openChild(namespaceURI, qName, attributes);
        } else {
            currentExtensionCollection.openChild(namespaceURI, qName, attributes);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        /*
         * Remove illegal characters generated by the Nokia Sports Tracker device.
         * Don't do this crude substitution for all files, since it would destroy
         * certain unicode characters.
         */
        if (nokiaSportsTrackerBug) {
            for (int i = 0; i < ch.length; ++i) {
                if (ch[i] == 1) {
                    ch[i] = 32;
                }
            }
            nokiaSportsTrackerBug = false;
        }

        accumulator.append(ch, start, length);
    }

    /**
     * Get the current attributes
     * @return The current attributes, if available
     */
    private Optional<Map<String, Object>> getAttr() {
        switch (currentState) {
            case RTE: return Optional.ofNullable(currentRoute.attr);
            case METADATA: return Optional.ofNullable(data.attr);
            case WPT: return Optional.ofNullable(currentWayPoint.attr);
            case TRK: return Optional.ofNullable(currentTrackAttr);
            default: return Optional.empty();
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        elements.pop();
        switch (currentState) {
            case GPX:       // GPX 1.0
            case METADATA:  // GPX 1.1
                endElementMetadata(localName);
                break;
            case AUTHOR:
                endElementAuthor(localName);
                break;
            case COPYRIGHT:
                endElementCopyright(localName);
                break;
            case LINK:
                endElementLink(localName);
                break;
            case WPT:
                endElementWpt(localName);
                break;
            case TRKSEG:
                endElementTrkseg(localName);
                break;
            case TRK:
                endElementTrk(localName);
                break;
            case EXT:
                endElementExt(localName, qName);
                break;
            default:
                endElementDefault(localName);
        }
        accumulator.setLength(0);
    }

    /**
     * End the metadata element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @see #startElementMetadata(String, Attributes)
     */
    private void endElementMetadata(String localName) {
        switch (localName) {
            case "name":
                data.put(META_NAME, accumulator.toString());
                break;
            case "desc":
                data.put(META_DESC, accumulator.toString());
                break;
            case "time":
                data.put(META_TIME, accumulator.toString());
                break;
            case "keywords":
                data.put(META_KEYWORDS, accumulator.toString());
                break;
            case "author":
                if ("1.0".equals(version)) {
                    // author is a string in 1.0, but complex element in 1.1
                    data.put(META_AUTHOR_NAME, accumulator.toString());
                }
                break;
            case "email":
                if ("1.0".equals(version)) {
                    data.put(META_AUTHOR_EMAIL, accumulator.toString());
                }
                break;
            case "url":
            case "urlname":
                data.put(localName, accumulator.toString());
                break;
            case "metadata":
            case "gpx":
                if ((currentState == State.METADATA && "metadata".equals(localName)) ||
                        (currentState == State.GPX && "gpx".equals(localName))) {
                    convertUrlToLink(data.attr);
                    data.getExtensions().addAll(currentExtensionCollection);
                    currentExtensionCollection.clear();
                    currentState = states.pop();
                }
                break;
            case "bounds":
                // do nothing, has been parsed on startElement
                break;
            default:
        }
    }

    /**
     * End the author element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @see #startElementAuthor(String, Attributes)
     */
    private void endElementAuthor(String localName) {
        switch (localName) {
            case "author":
                currentState = states.pop();
                break;
            case "name":
                data.put(META_AUTHOR_NAME, accumulator.toString());
                break;
            case "email":
                // do nothing, has been parsed on startElement
                break;
            case "link":
                data.put(META_AUTHOR_LINK, currentLink);
                break;
            default: // Do nothing
        }
    }

    /**
     * End the copyright element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     */
    private void endElementCopyright(String localName) {
        switch (localName) {
            case "copyright":
                currentState = states.pop();
                break;
            case "year":
                data.put(META_COPYRIGHT_YEAR, accumulator.toString());
                break;
            case "license":
                data.put(META_COPYRIGHT_LICENSE, accumulator.toString());
                break;
            default: // Do nothing
        }
    }

    /**
     * End a link element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     */
    @SuppressWarnings("unchecked")
    private void endElementLink(String localName) {
        switch (localName) {
            case "text":
                currentLink.text = accumulator.toString();
                break;
            case "type":
                currentLink.type = accumulator.toString();
                break;
            case "link":
                if (currentLink.uri == null && !accumulator.toString().isEmpty()) {
                    currentLink = new GpxLink(accumulator.toString());
                }
                currentState = states.pop();
                break;
            default: // Do nothing
        }
        if (currentState == State.AUTHOR) {
            data.put(META_AUTHOR_LINK, currentLink);
        } else if (currentState != State.LINK) {
            getAttr().ifPresent(attr ->
                ((Collection<GpxLink>) attr.computeIfAbsent(META_LINKS, e -> new LinkedList<GpxLink>())).add(currentLink));
        }
    }

    /**
     * End a wpt element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @throws SAXException If a waypoint does not have valid coordinates
     * @see #startElementWpt(String, Attributes)
     */
    private void endElementWpt(String localName) throws SAXException {
        switch (localName) {
            case "ele":
            case "magvar":
            case "name":
            case "src":
            case "geoidheight":
            case "type":
            case "sym":
            case "url":
            case "urlname":
            case "cmt":
            case "desc":
            case "fix":
                currentWayPoint.put(localName, accumulator.toString());
                break;
            case "hdop":
            case "vdop":
            case "pdop":
                try {
                    currentWayPoint.put(localName, Float.valueOf(accumulator.toString()));
                } catch (NumberFormatException e) {
                    currentWayPoint.put(localName, 0f);
                }
                break;
            case PT_TIME:
                try {
                    currentWayPoint.setInstant(DateUtils.parseInstant(accumulator.toString()));
                } catch (UncheckedParseException | DateTimeException e) {
                    Logging.error(e);
                }
                break;
            case "rtept":
                currentState = states.pop();
                convertUrlToLink(currentWayPoint.attr);
                currentRoute.routePoints.add(currentWayPoint);
                break;
            case "trkpt":
                currentState = states.pop();
                convertUrlToLink(currentWayPoint.attr);
                currentTrackSeg.add(currentWayPoint);
                break;
            case "wpt":
                currentState = states.pop();
                convertUrlToLink(currentWayPoint.attr);
                currentWayPoint.getExtensions().addAll(currentExtensionCollection);
                data.waypoints.add(currentWayPoint);
                currentExtensionCollection.clear();
                break;
            default: // Do nothing
        }
    }

    /**
     * End a trkseg element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @see #startElementTrkSeg(String, Attributes)
     */
    private void endElementTrkseg(String localName) {
        if ("trkseg".equals(localName)) {
            currentState = states.pop();
            if (!currentTrackSeg.isEmpty()) {
                GpxTrackSegment seg = new GpxTrackSegment(currentTrackSeg);
                if (!currentExtensionCollection.isEmpty()) {
                    seg.getExtensions().addAll(currentExtensionCollection);
                }
                currentTrack.add(seg);
            }
            currentExtensionCollection.clear();
        }
    }

    /**
     * End a trk element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @see #startElementTrk(String, Attributes)
     */
    private void endElementTrk(String localName) {
        switch (localName) {
            case "trk":
                currentState = states.pop();
                convertUrlToLink(currentTrackAttr);
                GpxTrack trk = new GpxTrack(new ArrayList<>(currentTrack), currentTrackAttr);
                if (!currentTrackExtensionCollection.isEmpty()) {
                    trk.getExtensions().addAll(currentTrackExtensionCollection);
                }
                data.addTrack(trk);
                currentTrackExtensionCollection.clear();
                break;
            case "name":
            case "cmt":
            case "desc":
            case "src":
            case "type":
            case "number":
            case "url":
            case "urlname":
                currentTrackAttr.put(localName, accumulator.toString());
                break;
            default: // Do nothing
        }
    }

    /**
     * End an ext element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
     * @see #startElementExt(String, String, Attributes)
     */
    private void endElementExt(String localName, String qName) {
        if ("extensions".equals(localName)) {
            currentState = states.pop();
        } else if (currentExtensionCollection != null) {
            String acc = accumulator.toString().trim();
            if (states.lastElement() == State.TRK) {
                currentTrackExtensionCollection.closeChild(qName, acc); //a segment inside the track can have an extension too
            } else {
                currentExtensionCollection.closeChild(qName, acc);
            }
        }
    }

    /**
     * End the default element
     * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
     */
    private void endElementDefault(String localName) {
        switch (localName) {
            case "wpt":
                currentState = states.pop();
                break;
            case "rte":
                currentState = states.pop();
                convertUrlToLink(currentRoute.attr);
                data.addRoute(currentRoute);
                break;
            default: // Do nothing
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (!states.isEmpty())
            throw new SAXException(tr("Parse error: invalid document structure for GPX document."));

        data.getExtensions().stream("josm", "from-server").findAny()
                .ifPresent(ext -> data.fromServer = "true".equals(ext.getValue()));

        data.getExtensions().stream("josm", "layerPreferences")
                .forEach(prefs -> prefs.getExtensions().stream("josm", "entry").forEach(prefEntry -> {
                    Object key = prefEntry.get("key");
                    Object val = prefEntry.get("value");
                    if (key != null && val != null) {
                        data.getLayerPrefs().put(key.toString(), val.toString());
                    }
                }));
        data.endUpdate();
    }

    /**
     * Get the parsed {@link GpxData}
     * @return The data
     */
    GpxData getData() {
        return this.data;
    }

    /**
     * convert url/urlname to link element (GPX 1.0 -&gt; GPX 1.1).
     * @param attr attributes
     */
    private static void convertUrlToLink(Map<String, Object> attr) {
        String url = (String) attr.get("url");
        String urlname = (String) attr.get("urlname");
        if (url != null) {
            GpxLink link = new GpxLink(url);
            link.text = urlname;
            @SuppressWarnings("unchecked")
            Collection<GpxLink> links = (Collection<GpxLink>) attr.computeIfAbsent(META_LINKS, e -> new LinkedList<>());
            links.add(link);
        }
    }

    /**
     * Attempt to finish parsing
     * @throws SAXException If there are additional parsing errors
     */
    void tryToFinish() throws SAXException {
        List<String[]> remainingElements = new ArrayList<>(elements);
        for (int i = remainingElements.size() - 1; i >= 0; i--) {
            String[] e = remainingElements.get(i);
            endElement(e[0], e[1], e[2]);
        }
        endDocument();
    }
}
