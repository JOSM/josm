//License: GPL. For details, see LICENSE file.

//TODO: this is far from complete, but can emulate old RawGps behaviour
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
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.Extensions;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.ImmutableGpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Read a gpx file.
 *
 * Bounds are not read, as we caluclate them. @see GpxData.recalculateBounds()
 * Both GPX version 1.0 and 1.1 are supported.
 *
 * @author imi, ramack
 */
public class GpxReader implements GpxConstants {

    private String version;
    /**
     * The resulting gpx data
     */
    private GpxData gpxData;
    private enum State { init, gpx, metadata, wpt, rte, trk, ext, author, link, trkseg, copyright}
    private InputSource inputSource;

    private class Parser extends DefaultHandler {

        private GpxData data;
        private Collection<Collection<WayPoint>> currentTrack;
        private Map<String, Object> currentTrackAttr;
        private Collection<WayPoint> currentTrackSeg;
        private GpxRoute currentRoute;
        private WayPoint currentWayPoint;

        private State currentState = State.init;

        private GpxLink currentLink;
        private Extensions currentExtensions;
        private Stack<State> states;
        private final Stack<String> elements = new Stack<String>();

        private StringBuffer accumulator = new StringBuffer();

        private boolean nokiaSportsTrackerBug = false;

        @Override public void startDocument() {
            accumulator = new StringBuffer();
            states = new Stack<State>();
            data = new GpxData();
        }

        private double parseCoord(String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                return Double.NaN;
            }
        }

        private LatLon parseLatLon(Attributes atts) {
            return new LatLon(
                    parseCoord(atts.getValue("lat")),
                    parseCoord(atts.getValue("lon")));
        }

        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            elements.push(localName);
            switch(currentState) {
            case init:
                states.push(currentState);
                currentState = State.gpx;
                data.creator = atts.getValue("creator");
                version = atts.getValue("version");
                if (version != null && version.startsWith("1.0")) {
                    version = "1.0";
                } else if (!"1.1".equals(version)) {
                    // unknown version, assume 1.1
                    version = "1.1";
                }
                break;
            case gpx:
                if (localName.equals("metadata")) {
                    states.push(currentState);
                    currentState = State.metadata;
                } else if (localName.equals("wpt")) {
                    states.push(currentState);
                    currentState = State.wpt;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                } else if (localName.equals("rte")) {
                    states.push(currentState);
                    currentState = State.rte;
                    currentRoute = new GpxRoute();
                } else if (localName.equals("trk")) {
                    states.push(currentState);
                    currentState = State.trk;
                    currentTrack = new ArrayList<Collection<WayPoint>>();
                    currentTrackAttr = new HashMap<String, Object>();
                } else if (localName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                    currentExtensions = new Extensions();
                } else if (localName.equals("gpx") && atts.getValue("creator") != null && atts.getValue("creator").startsWith("Nokia Sports Tracker")) {
                    nokiaSportsTrackerBug = true;
                }
                break;
            case metadata:
                if (localName.equals("author")) {
                    states.push(currentState);
                    currentState = State.author;
                } else if (localName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                    currentExtensions = new Extensions();
                } else if (localName.equals("copyright")) {
                    states.push(currentState);
                    currentState = State.copyright;
                    data.attr.put(META_COPYRIGHT_AUTHOR, atts.getValue("author"));
                } else if (localName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                }
                break;
            case author:
                if (localName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (localName.equals("email")) {
                    data.attr.put(META_AUTHOR_EMAIL, atts.getValue("id") + "@" + atts.getValue("domain"));
                }
                break;
            case trk:
                if (localName.equals("trkseg")) {
                    states.push(currentState);
                    currentState = State.trkseg;
                    currentTrackSeg = new ArrayList<WayPoint>();
                } else if (localName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (localName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                    currentExtensions = new Extensions();
                }
                break;
            case trkseg:
                if (localName.equals("trkpt")) {
                    states.push(currentState);
                    currentState = State.wpt;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                }
                break;
            case wpt:
                if (localName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (localName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                    currentExtensions = new Extensions();
                }
                break;
            case rte:
                if (localName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (localName.equals("rtept")) {
                    states.push(currentState);
                    currentState = State.wpt;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                } else if (localName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                    currentExtensions = new Extensions();
                }
                break;
            }
            accumulator.setLength(0);
        }

        @Override public void characters(char[] ch, int start, int length) {
            /**
             * Remove illegal characters generated by the Nokia Sports Tracker device.
             * Don't do this crude substitution for all files, since it would destroy
             * certain unicode characters.
             */
            if (nokiaSportsTrackerBug) {
                for (int i=0; i<ch.length; ++i) {
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
            case rte: return currentRoute.attr;
            case metadata: return data.attr;
            case wpt: return currentWayPoint.attr;
            case trk: return currentTrackAttr;
            default: return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override public void endElement(String namespaceURI, String localName, String qName) {
            elements.pop();
            switch (currentState) {
            case gpx:       // GPX 1.0
            case metadata:  // GPX 1.1
                if (localName.equals("name")) {
                    data.attr.put(META_NAME, accumulator.toString());
                } else if (localName.equals("desc")) {
                    data.attr.put(META_DESC, accumulator.toString());
                } else if (localName.equals("time")) {
                    data.attr.put(META_TIME, accumulator.toString());
                } else if (localName.equals("keywords")) {
                    data.attr.put(META_KEYWORDS, accumulator.toString());
                } else if (version.equals("1.0") && localName.equals("author")) {
                    // author is a string in 1.0, but complex element in 1.1
                    data.attr.put(META_AUTHOR_NAME, accumulator.toString());
                } else if (version.equals("1.0") && localName.equals("email")) {
                    data.attr.put(META_AUTHOR_EMAIL, accumulator.toString());
                } else if (localName.equals("url") || localName.equals("urlname")) {
                    data.attr.put(localName, accumulator.toString());
                } else if ((currentState == State.metadata && localName.equals("metadata")) ||
                        (currentState == State.gpx && localName.equals("gpx"))) {
                    convertUrlToLink(data.attr);
                    if (currentExtensions != null && !currentExtensions.isEmpty()) {
                        data.attr.put(META_EXTENSIONS, currentExtensions);
                    }
                    currentState = states.pop();
                }
                //TODO: parse bounds, extensions
                break;
            case author:
                if (localName.equals("author")) {
                    currentState = states.pop();
                } else if (localName.equals("name")) {
                    data.attr.put(META_AUTHOR_NAME, accumulator.toString());
                } else if (localName.equals("email")) {
                    // do nothing, has been parsed on startElement
                } else if (localName.equals("link")) {
                    data.attr.put(META_AUTHOR_LINK, currentLink);
                }
                break;
            case copyright:
                if (localName.equals("copyright")) {
                    currentState = states.pop();
                } else if (localName.equals("year")) {
                    data.attr.put(META_COPYRIGHT_YEAR, accumulator.toString());
                } else if (localName.equals("license")) {
                    data.attr.put(META_COPYRIGHT_LICENSE, accumulator.toString());
                }
                break;
            case link:
                if (localName.equals("text")) {
                    currentLink.text = accumulator.toString();
                } else if (localName.equals("type")) {
                    currentLink.type = accumulator.toString();
                } else if (localName.equals("link")) {
                    if (currentLink.uri == null && accumulator != null && accumulator.toString().length() != 0) {
                        currentLink = new GpxLink(accumulator.toString());
                    }
                    currentState = states.pop();
                }
                if (currentState == State.author) {
                    data.attr.put(META_AUTHOR_LINK, currentLink);
                } else if (currentState != State.link) {
                    Map<String, Object> attr = getAttr();
                    if (!attr.containsKey(META_LINKS)) {
                        attr.put(META_LINKS, new LinkedList<GpxLink>());
                    }
                    ((Collection<GpxLink>) attr.get(META_LINKS)).add(currentLink);
                }
                break;
            case wpt:
                if (   localName.equals("ele")  || localName.equals("magvar")
                        || localName.equals("name") || localName.equals("src")
                        || localName.equals("geoidheight") || localName.equals("type")
                        || localName.equals("sym") || localName.equals("url")
                        || localName.equals("urlname")) {
                    currentWayPoint.attr.put(localName, accumulator.toString());
                } else if(localName.equals("hdop") || localName.equals("vdop") ||
                        localName.equals("pdop")) {
                    try {
                        currentWayPoint.attr.put(localName, Float.parseFloat(accumulator.toString()));
                    } catch(Exception e) {
                        currentWayPoint.attr.put(localName, new Float(0));
                    }
                } else if (localName.equals("time")) {
                    currentWayPoint.attr.put(localName, accumulator.toString());
                    currentWayPoint.setTime();
                } else if (localName.equals("cmt") || localName.equals("desc")) {
                    currentWayPoint.attr.put(localName, accumulator.toString());
                    currentWayPoint.setTime();
                } else if (localName.equals("rtept")) {
                    currentState = states.pop();
                    convertUrlToLink(currentWayPoint.attr);
                    currentRoute.routePoints.add(currentWayPoint);
                } else if (localName.equals("trkpt")) {
                    currentState = states.pop();
                    convertUrlToLink(currentWayPoint.attr);
                    currentTrackSeg.add(currentWayPoint);
                } else if (localName.equals("wpt")) {
                    currentState = states.pop();
                    convertUrlToLink(currentWayPoint.attr);
                    if (currentExtensions != null && !currentExtensions.isEmpty()) {
                        currentWayPoint.attr.put(META_EXTENSIONS, currentExtensions);
                    }
                    data.waypoints.add(currentWayPoint);
                }
                break;
            case trkseg:
                if (localName.equals("trkseg")) {
                    currentState = states.pop();
                    currentTrack.add(currentTrackSeg);
                }
                break;
            case trk:
                if (localName.equals("trk")) {
                    currentState = states.pop();
                    convertUrlToLink(currentTrackAttr);
                    data.tracks.add(new ImmutableGpxTrack(currentTrack, currentTrackAttr));
                } else if (localName.equals("name") || localName.equals("cmt")
                        || localName.equals("desc") || localName.equals("src")
                        || localName.equals("type") || localName.equals("number")
                        || localName.equals("url") || localName.equals("urlname")) {
                    currentTrackAttr.put(localName, accumulator.toString());
                }
                break;
            case ext:
                if (localName.equals("extensions")) {
                    currentState = states.pop();
                // only interested in extensions written by JOSM
                } else if (JOSM_EXTENSIONS_NAMESPACE_URI.equals(namespaceURI)) {
                    currentExtensions.put(localName, accumulator.toString());
                }
                break;
            default:
                if (localName.equals("wpt")) {
                    currentState = states.pop();
                } else if (localName.equals("rte")) {
                    currentState = states.pop();
                    convertUrlToLink(currentRoute.attr);
                    data.routes.add(currentRoute);
                }
            }
        }

        @Override public void endDocument() throws SAXException  {
            if (!states.empty())
                throw new SAXException(tr("Parse error: invalid document structure for GPX document."));
            Extensions metaExt = (Extensions) data.attr.get(META_EXTENSIONS);
            if (metaExt != null && "true".equals(metaExt.get("from-server"))) {
                data.fromServer = true;
            }
            gpxData = data;
        }

        /**
         * convert url/urlname to link element (GPX 1.0 -&gt; GPX 1.1).
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
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Collection<GpxLink> links = (Collection<GpxLink>) attr.get(META_LINKS);
                links.add(link);
            }
        }

        public void tryToFinish() throws SAXException {
            List<String> remainingElements = new ArrayList<String>(elements);
            for (int i=remainingElements.size() - 1; i >= 0; i--) {
                endElement(null, remainingElements.get(i), remainingElements.get(i));
            }
            endDocument();
        }
    }

    /**
     * Parse the input stream and store the result in trackData and markerData
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
     * @throws SAXException
     * @throws IOException
     */
    public boolean parse(boolean tryToFinish) throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.newSAXParser().parse(inputSource, parser);
            return true;
        } catch (SAXException e) {
            if (tryToFinish) {
                parser.tryToFinish();
                if (parser.data.isEmpty())
                    throw e;
                String message = e.getMessage();
                if (e instanceof SAXParseException) {
                    SAXParseException spe = ((SAXParseException)e);
                    message += " " + tr("(at line {0}, column {1})", spe.getLineNumber(), spe.getColumnNumber());
                }
                Main.warn(message);
                return false;
            } else
                throw e;
        } catch (ParserConfigurationException e) {
            Main.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
    }

    /**
     * Replies the GPX data.
     * @return The GPX data
     */
    public GpxData getGpxData() {
        return gpxData;
    }
}
