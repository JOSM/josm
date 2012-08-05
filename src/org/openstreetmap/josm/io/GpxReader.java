//License: GPL. Copyright 2007 by Immanuel Scholz and others

//TODO: this is far from complete, but can emulate old RawGps behaviour
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.ImmutableGpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Read a gpx file.
 * 
 * Bounds are not read, as we caluclate them. @see GpxData.recalculateBounds()
 * Both GPX version 1.0 and 1.1 are supported.
 *
 * @author imi, ramack
 */
public class GpxReader {

    private String version;
    /**
     * The resulting gpx data
     */
    public GpxData data;
    private enum State { init, gpx, metadata, wpt, rte, trk, ext, author, link, trkseg, copyright}
    private InputSource inputSource;

    private class Parser extends DefaultHandler {

        private GpxData currentData;
        private Collection<Collection<WayPoint>> currentTrack;
        private Map<String, Object> currentTrackAttr;
        private Collection<WayPoint> currentTrackSeg;
        private GpxRoute currentRoute;
        private WayPoint currentWayPoint;

        private State currentState = State.init;

        private GpxLink currentLink;
        private Stack<State> states;
        private final Stack<String> elements = new Stack<String>();

        private StringBuffer accumulator = new StringBuffer();

        private boolean nokiaSportsTrackerBug = false;

        @Override public void startDocument() {
            accumulator = new StringBuffer();
            states = new Stack<State>();
            currentData = new GpxData();
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
            elements.push(qName);
            switch(currentState) {
            case init:
                states.push(currentState);
                currentState = State.gpx;
                currentData.creator = atts.getValue("creator");
                version = atts.getValue("version");
                if (version != null && version.startsWith("1.0")) {
                    version = "1.0";
                } else if (!"1.1".equals(version)) {
                    // unknown version, assume 1.1
                    version = "1.1";
                }
                break;
            case gpx:
                if (qName.equals("metadata")) {
                    states.push(currentState);
                    currentState = State.metadata;
                } else if (qName.equals("wpt")) {
                    states.push(currentState);
                    currentState = State.wpt;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                } else if (qName.equals("rte")) {
                    states.push(currentState);
                    currentState = State.rte;
                    currentRoute = new GpxRoute();
                } else if (qName.equals("trk")) {
                    states.push(currentState);
                    currentState = State.trk;
                    currentTrack = new ArrayList<Collection<WayPoint>>();
                    currentTrackAttr = new HashMap<String, Object>();
                } else if (qName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                } else if (qName.equals("gpx") && atts.getValue("creator") != null && atts.getValue("creator").startsWith("Nokia Sports Tracker")) {
                    nokiaSportsTrackerBug = true;
                }
                break;
            case metadata:
                if (qName.equals("author")) {
                    states.push(currentState);
                    currentState = State.author;
                } else if (qName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                } else if (qName.equals("copyright")) {
                    states.push(currentState);
                    currentState = State.copyright;
                    currentData.attr.put(GpxData.META_COPYRIGHT_AUTHOR, atts.getValue("author"));
                } else if (qName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                }
                break;
            case author:
                if (qName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (qName.equals("email")) {
                    currentData.attr.put(GpxData.META_AUTHOR_EMAIL, atts.getValue("id") + "@" + atts.getValue("domain"));
                }
                break;
            case trk:
                if (qName.equals("trkseg")) {
                    states.push(currentState);
                    currentState = State.trkseg;
                    currentTrackSeg = new ArrayList<WayPoint>();
                } else if (qName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (qName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                }
                break;
            case trkseg:
                if (qName.equals("trkpt")) {
                    states.push(currentState);
                    currentState = State.wpt;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                }
                break;
            case wpt:
                if (qName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (qName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                }
                break;
            case rte:
                if (qName.equals("link")) {
                    states.push(currentState);
                    currentState = State.link;
                    currentLink = new GpxLink(atts.getValue("href"));
                } else if (qName.equals("rtept")) {
                    states.push(currentState);
                    currentState = State.wpt;
                    currentWayPoint = new WayPoint(parseLatLon(atts));
                } else if (qName.equals("extensions")) {
                    states.push(currentState);
                    currentState = State.ext;
                }
                break;
            default:
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
            case metadata: return currentData.attr;
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
                if (qName.equals("name")) {
                    currentData.attr.put(GpxData.META_NAME, accumulator.toString());
                } else if (qName.equals("desc")) {
                    currentData.attr.put(GpxData.META_DESC, accumulator.toString());
                } else if (qName.equals("time")) {
                    currentData.attr.put(GpxData.META_TIME, accumulator.toString());
                } else if (qName.equals("keywords")) {
                    currentData.attr.put(GpxData.META_KEYWORDS, accumulator.toString());
                } else if (version.equals("1.0") && qName.equals("author")) {
                    // author is a string in 1.0, but complex element in 1.1
                    currentData.attr.put(GpxData.META_AUTHOR_NAME, accumulator.toString());
                } else if (version.equals("1.0") && qName.equals("email")) {
                    currentData.attr.put(GpxData.META_AUTHOR_EMAIL, accumulator.toString());
                } else if (qName.equals("url") || qName.equals("urlname")) {
                    currentData.attr.put(qName, accumulator.toString());
                } else if ((currentState == State.metadata && qName.equals("metadata")) ||
                        (currentState == State.gpx && qName.equals("gpx"))) {
                    convertUrlToLink(currentData.attr);
                    currentState = states.pop();
                }
                //TODO: parse bounds, extensions
                break;
            case author:
                if (qName.equals("author")) {
                    currentState = states.pop();
                } else if (qName.equals("name")) {
                    currentData.attr.put(GpxData.META_AUTHOR_NAME, accumulator.toString());
                } else if (qName.equals("email")) {
                    // do nothing, has been parsed on startElement
                } else if (qName.equals("link")) {
                    currentData.attr.put(GpxData.META_AUTHOR_LINK, currentLink);
                }
                break;
            case copyright:
                if (qName.equals("copyright")) {
                    currentState = states.pop();
                } else if (qName.equals("year")) {
                    currentData.attr.put(GpxData.META_COPYRIGHT_YEAR, accumulator.toString());
                } else if (qName.equals("license")) {
                    currentData.attr.put(GpxData.META_COPYRIGHT_LICENSE, accumulator.toString());
                }
                break;
            case link:
                if (qName.equals("text")) {
                    currentLink.text = accumulator.toString();
                } else if (qName.equals("type")) {
                    currentLink.type = accumulator.toString();
                } else if (qName.equals("link")) {
                    if (currentLink.uri == null && accumulator != null && accumulator.toString().length() != 0) {
                        currentLink = new GpxLink(accumulator.toString());
                    }
                    currentState = states.pop();
                }
                if (currentState == State.author) {
                    currentData.attr.put(GpxData.META_AUTHOR_LINK, currentLink);
                } else if (currentState != State.link) {
                    Map<String, Object> attr = getAttr();
                    if (!attr.containsKey(GpxData.META_LINKS)) {
                        attr.put(GpxData.META_LINKS, new LinkedList<GpxLink>());
                    }
                    ((Collection<GpxLink>) attr.get(GpxData.META_LINKS)).add(currentLink);
                }
                break;
            case wpt:
                if (   qName.equals("ele")  || qName.equals("magvar")
                        || qName.equals("name") || qName.equals("src")
                        || qName.equals("geoidheight") || qName.equals("type")
                        || qName.equals("sym") || qName.equals("url")
                        || qName.equals("urlname")) {
                    currentWayPoint.attr.put(qName, accumulator.toString());
                } else if(qName.equals("hdop") || qName.equals("vdop") ||
                        qName.equals("pdop")) {
                    try {
                        currentWayPoint.attr.put(qName, Float.parseFloat(accumulator.toString()));
                    } catch(Exception e) {
                        currentWayPoint.attr.put(qName, new Float(0));
                    }
                } else if (qName.equals("time")) {
                    currentWayPoint.attr.put(qName, accumulator.toString());
                    currentWayPoint.setTime();
                } else if (qName.equals("cmt") || qName.equals("desc")) {
                    currentWayPoint.attr.put(qName, accumulator.toString());
                    currentWayPoint.setTime();
                } else if (qName.equals("rtept")) {
                    currentState = states.pop();
                    convertUrlToLink(currentWayPoint.attr);
                    currentRoute.routePoints.add(currentWayPoint);
                } else if (qName.equals("trkpt")) {
                    currentState = states.pop();
                    convertUrlToLink(currentWayPoint.attr);
                    currentTrackSeg.add(currentWayPoint);
                } else if (qName.equals("wpt")) {
                    currentState = states.pop();
                    convertUrlToLink(currentWayPoint.attr);
                    currentData.waypoints.add(currentWayPoint);
                }
                break;
            case trkseg:
                if (qName.equals("trkseg")) {
                    currentState = states.pop();
                    currentTrack.add(currentTrackSeg);
                }
                break;
            case trk:
                if (qName.equals("trk")) {
                    currentState = states.pop();
                    convertUrlToLink(currentTrackAttr);
                    currentData.tracks.add(new ImmutableGpxTrack(currentTrack, currentTrackAttr));
                } else if (qName.equals("name") || qName.equals("cmt")
                        || qName.equals("desc") || qName.equals("src")
                        || qName.equals("type") || qName.equals("number")
                        || qName.equals("url") || qName.equals("urlname")) {
                    currentTrackAttr.put(qName, accumulator.toString());
                }
                break;
            case ext:
                if (qName.equals("extensions")) {
                    currentState = states.pop();
                }
                break;
            default:
                if (qName.equals("wpt")) {
                    currentState = states.pop();
                } else if (qName.equals("rte")) {
                    currentState = states.pop();
                    convertUrlToLink(currentRoute.attr);
                    currentData.routes.add(currentRoute);
                }
            }
        }

        @Override public void endDocument() throws SAXException  {
            if (!states.empty())
                throw new SAXException(tr("Parse error: invalid document structure for GPX document."));
            data = currentData;
        }

        /**
         * convert url/urlname to link element (GPX 1.0 -> GPX 1.1).
         */
        private void convertUrlToLink(Map<String, Object> attr) {
            String url = (String) attr.get("url");
            String urlname = (String) attr.get("urlname");
            if (url != null) {
                if (!attr.containsKey(GpxData.META_LINKS)) {
                    attr.put(GpxData.META_LINKS, new LinkedList<GpxLink>());
                }
                GpxLink link = new GpxLink(url);
                link.text = urlname;
                ((Collection<GpxLink>) attr.get(GpxData.META_LINKS)).add(link);
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
     */
    public GpxReader(InputStream source) throws IOException {
        this.inputSource = new InputSource(UTFInputStreamReader.create(source, "UTF-8"));
    }

    /**
     *
     * @return True if file was properly parsed, false if there was error during parsing but some data were parsed anyway
     * @throws SAXException
     * @throws IOException
     */
    public boolean parse(boolean tryToFinish) throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // support files with invalid xml namespace declarations (see #7247)
            factory.setNamespaceAware(false);
            factory.newSAXParser().parse(inputSource, parser);
            return true;
        } catch (SAXException e) {
            if (tryToFinish) {
                parser.tryToFinish();
                if (parser.currentData.isEmpty())
                    throw e;
                return false;
            } else
                throw e;
        } catch (ParserConfigurationException e) {
            e.printStackTrace(); // broken SAXException chaining
            throw new SAXException(e);
        }
    }
}
