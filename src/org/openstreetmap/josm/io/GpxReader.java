// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.Extensions;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.ImmutableGpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Read a gpx file.
 *
 * Bounds are read, even if we calculate them, see {@link GpxData#recalculateBounds}.<br>
 * Both GPX version 1.0 and 1.1 are supported.
 *
 * @author imi, ramack
 */
public class GpxReader implements GpxConstants, IGpxReader {

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
    /** The resulting gpx data */
    private GpxData gpxData;
    private final InputSource inputSource;

    private class Parser extends DefaultHandler {

        private GpxData data;
        private Collection<Collection<WayPoint>> currentTrack;
        private Map<String, Object> currentTrackAttr;
        private Collection<WayPoint> currentTrackSeg;
        private GpxRoute currentRoute;
        private WayPoint currentWayPoint;

        private State currentState = State.INIT;

        private GpxLink currentLink;
        private Extensions currentExtensions;
        private Stack<State> states;
        private final Stack<String> elements = new Stack<>();

        private StringBuilder accumulator = new StringBuilder();

        private boolean nokiaSportsTrackerBug;

        @Override
        public void startDocument() {
            accumulator = new StringBuilder();
            states = new Stack<>();
            data = new GpxData();
        }

        private double parseCoord(Attributes atts, String key) {
            String val = atts.getValue(key);
            if (val != null) {
                return parseCoord(val);
            } else {
                // Some software do not respect GPX schema and use "minLat" / "minLon" instead of "minlat" / "minlon"
                return parseCoord(atts.getValue(key.replaceFirst("l", "L")));
            }
        }

        private double parseCoord(String s) {
            if (s != null) {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException ex) {
                    Logging.trace(ex);
                }
            }
            return Double.NaN;
        }

        private LatLon parseLatLon(Attributes atts) {
            return new LatLon(
                    parseCoord(atts, "lat"),
                    parseCoord(atts, "lon"));
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            elements.push(localName);
            switch(currentState) {
            case INIT:
                states.push(currentState);
                currentState = State.GPX;
                data.creator = atts.getValue("creator");
                version = atts.getValue("version");
                if (version != null && version.startsWith("1.0")) {
                    version = "1.0";
                } else if (!"1.1".equals(version)) {
                    // unknown version, assume 1.1
                    version = "1.1";
                }
                break;
            case GPX:
                switch (localName) {
                case "metadata":
                    states.push(currentState);
                    currentState = State.METADATA;
                    break;
                case "wpt":
                    states.push(currentState);
                    currentState = State.WPT;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
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
                    currentExtensions = new Extensions();
                    break;
                case "gpx":
                    if (atts.getValue("creator") != null && atts.getValue("creator").startsWith("Nokia Sports Tracker")) {
                        nokiaSportsTrackerBug = true;
                    }
                    break;
                default: // Do nothing
                }
                break;
            case METADATA:
                switch (localName) {
                case "author":
                    states.push(currentState);
                    currentState = State.AUTHOR;
                    break;
                case "extensions":
                    states.push(currentState);
                    currentState = State.EXT;
                    currentExtensions = new Extensions();
                    break;
                case "copyright":
                    states.push(currentState);
                    currentState = State.COPYRIGHT;
                    data.put(META_COPYRIGHT_AUTHOR, atts.getValue("author"));
                    break;
                case "link":
                    states.push(currentState);
                    currentState = State.LINK;
                    currentLink = new GpxLink(atts.getValue("href"));
                    break;
                case "bounds":
                    data.put(META_BOUNDS, new Bounds(
                                parseCoord(atts, "minlat"),
                                parseCoord(atts, "minlon"),
                                parseCoord(atts, "maxlat"),
                                parseCoord(atts, "maxlon")));
                    break;
                default: // Do nothing
                }
                break;
            case AUTHOR:
                switch (localName) {
                case "link":
                    states.push(currentState);
                    currentState = State.LINK;
                    currentLink = new GpxLink(atts.getValue("href"));
                    break;
                case "email":
                    data.put(META_AUTHOR_EMAIL, atts.getValue("id") + '@' + atts.getValue("domain"));
                    break;
                default: // Do nothing
                }
                break;
            case TRK:
                switch (localName) {
                case "trkseg":
                    states.push(currentState);
                    currentState = State.TRKSEG;
                    currentTrackSeg = new ArrayList<>();
                    break;
                case "link":
                    states.push(currentState);
                    currentState = State.LINK;
                    currentLink = new GpxLink(atts.getValue("href"));
                    break;
                case "extensions":
                    states.push(currentState);
                    currentState = State.EXT;
                    currentExtensions = new Extensions();
                    break;
                default: // Do nothing
                }
                break;
            case TRKSEG:
                if ("trkpt".equals(localName)) {
                    states.push(currentState);
                    currentState = State.WPT;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                }
                break;
            case WPT:
                switch (localName) {
                case "link":
                    states.push(currentState);
                    currentState = State.LINK;
                    currentLink = new GpxLink(atts.getValue("href"));
                    break;
                case "extensions":
                    states.push(currentState);
                    currentState = State.EXT;
                    currentExtensions = new Extensions();
                    break;
                default: // Do nothing
                }
                break;
            case RTE:
                switch (localName) {
                case "link":
                    states.push(currentState);
                    currentState = State.LINK;
                    currentLink = new GpxLink(atts.getValue("href"));
                    break;
                case "rtept":
                    states.push(currentState);
                    currentState = State.WPT;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                    break;
                case "extensions":
                    states.push(currentState);
                    currentState = State.EXT;
                    currentExtensions = new Extensions();
                    break;
                default: // Do nothing
                }
                break;
            default: // Do nothing
            }
            accumulator.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            /**
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

        private Map<String, Object> getAttr() {
            switch (currentState) {
            case RTE: return currentRoute.attr;
            case METADATA: return data.attr;
            case WPT: return currentWayPoint.attr;
            case TRK: return currentTrackAttr;
            default: return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            elements.pop();
            switch (currentState) {
            case GPX:       // GPX 1.0
            case METADATA:  // GPX 1.1
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
                        if (currentExtensions != null && !currentExtensions.isEmpty()) {
                            data.put(META_EXTENSIONS, currentExtensions);
                        }
                        currentState = states.pop();
                    }
                    break;
                case "bounds":
                    // do nothing, has been parsed on startElement
                    break;
                default:
                    //TODO: parse extensions
                }
                break;
            case AUTHOR:
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
                break;
            case COPYRIGHT:
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
                break;
            case LINK:
                switch (localName) {
                case "text":
                    currentLink.text = accumulator.toString();
                    break;
                case "type":
                    currentLink.type = accumulator.toString();
                    break;
                case "link":
                    if (currentLink.uri == null && accumulator != null && !accumulator.toString().isEmpty()) {
                        currentLink = new GpxLink(accumulator.toString());
                    }
                    currentState = states.pop();
                    break;
                default: // Do nothing
                }
                if (currentState == State.AUTHOR) {
                    data.put(META_AUTHOR_LINK, currentLink);
                } else if (currentState != State.LINK) {
                    Map<String, Object> attr = getAttr();
                    if (attr != null && !attr.containsKey(META_LINKS)) {
                        attr.put(META_LINKS, new LinkedList<GpxLink>());
                    }
                    if (attr != null)
                        ((Collection<GpxLink>) attr.get(META_LINKS)).add(currentLink);
                }
                break;
            case WPT:
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
                case "time":
                case "cmt":
                case "desc":
                    currentWayPoint.put(localName, accumulator.toString());
                    currentWayPoint.setTimeFromAttribute();
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
                    if (currentExtensions != null && !currentExtensions.isEmpty()) {
                        currentWayPoint.put(META_EXTENSIONS, currentExtensions);
                    }
                    data.waypoints.add(currentWayPoint);
                    break;
                default: // Do nothing
                }
                break;
            case TRKSEG:
                if ("trkseg".equals(localName)) {
                    currentState = states.pop();
                    currentTrack.add(currentTrackSeg);
                }
                break;
            case TRK:
                switch (localName) {
                case "trk":
                    currentState = states.pop();
                    convertUrlToLink(currentTrackAttr);
                    data.addTrack(new ImmutableGpxTrack(currentTrack, currentTrackAttr));
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
                break;
            case EXT:
                if ("extensions".equals(localName)) {
                    currentState = states.pop();
                } else if (JOSM_EXTENSIONS_NAMESPACE_URI.equals(namespaceURI)) {
                    // only interested in extensions written by JOSM
                    currentExtensions.put(localName, accumulator.toString());
                }
                break;
            default:
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
        }

        @Override
        public void endDocument() throws SAXException {
            if (!states.empty())
                throw new SAXException(tr("Parse error: invalid document structure for GPX document."));
            Extensions metaExt = (Extensions) data.get(META_EXTENSIONS);
            if (metaExt != null && "true".equals(metaExt.get("from-server"))) {
                data.fromServer = true;
            }
            gpxData = data;
        }

        /**
         * convert url/urlname to link element (GPX 1.0 -&gt; GPX 1.1).
         * @param attr attributes
         */
        private void convertUrlToLink(Map<String, Object> attr) {
            String url = (String) attr.get("url");
            String urlname = (String) attr.get("urlname");
            if (url != null) {
                if (!attr.containsKey(META_LINKS)) {
                    attr.put(META_LINKS, new LinkedList<GpxLink>());
                }
                GpxLink link = new GpxLink(url);
                link.text = urlname;
                @SuppressWarnings("unchecked")
                Collection<GpxLink> links = (Collection<GpxLink>) attr.get(META_LINKS);
                links.add(link);
            }
        }

        void tryToFinish() throws SAXException {
            List<String> remainingElements = new ArrayList<>(elements);
            for (int i = remainingElements.size() - 1; i >= 0; i--) {
                endElement(null, remainingElements.get(i), remainingElements.get(i));
            }
            endDocument();
        }
    }

    /**
     * Constructs a new {@code GpxReader}, which can later parse the input stream
     * and store the result in trackData and markerData
     *
     * @param source the source input stream
     * @throws IOException if an IO error occurs, e.g. the input stream is closed.
     */
    public GpxReader(InputStream source) throws IOException {
        Reader utf8stream = UTFInputStreamReader.create(source);
        Reader filtered = new InvalidXmlCharacterFilter(utf8stream);
        this.inputSource = new InputSource(filtered);
    }

    /**
     * Parse the GPX data.
     *
     * @param tryToFinish true, if the reader should return at least part of the GPX
     * data in case of an error.
     * @return true if file was properly parsed, false if there was error during
     * parsing but some data were parsed anyway
     * @throws SAXException if any SAX parsing error occurs
     * @throws IOException if any I/O error occurs
     */
    @Override
    public boolean parse(boolean tryToFinish) throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            XmlUtils.parseSafeSAX(inputSource, parser);
            return true;
        } catch (SAXException e) {
            if (tryToFinish) {
                parser.tryToFinish();
                if (parser.data.isEmpty())
                    throw e;
                String message = e.getMessage();
                if (e instanceof SAXParseException) {
                    SAXParseException spe = (SAXParseException) e;
                    message += ' ' + tr("(at line {0}, column {1})", spe.getLineNumber(), spe.getColumnNumber());
                }
                Logging.warn(message);
                return false;
            } else
                throw e;
        } catch (ParserConfigurationException e) {
            Logging.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
    }

    @Override
    public GpxData getGpxData() {
        return gpxData;
    }
}
