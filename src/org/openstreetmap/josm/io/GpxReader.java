//License: GPL. Copyright 2007 by Immanuel Scholz and others

//TODO: this is far from complete, but can emulate old RawGps behaviour
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Read a gpx file. Bounds are not read, as we caluclate them. @see GpxData.recalculateBounds()
 * @author imi, ramack
 */
public class GpxReader {
    // TODO: implement GPX 1.0 parsing

    /**
     * The resulting gpx data
     */
    public GpxData data;
    public enum state { init, metadata, wpt, rte, trk, ext, author, link, trkseg, copyright}

    private class Parser extends DefaultHandler {

        private GpxData currentData;
        private GpxTrack currentTrack;
        private Collection<WayPoint> currentTrackSeg;
        private GpxRoute currentRoute;
        private WayPoint currentWayPoint;

        private state currentState = state.init;

        private GpxLink currentLink;
        private Stack<state> states;

        private StringBuffer accumulator = new StringBuffer();

        private boolean nokiaSportsTrackerBug = false;

        @Override public void startDocument() {
            accumulator = new StringBuffer();
            states = new Stack<state>();
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

        @Override public void startElement(String namespaceURI, String qName, String rqName, Attributes atts) throws SAXException {
            switch(currentState) {
                case init:
                    if (qName.equals("metadata")) {
                        states.push(currentState);
                        currentState = state.metadata;
                    } else if (qName.equals("wpt")) {
                        states.push(currentState);
                        currentState = state.wpt;
                        currentWayPoint = new WayPoint(parseLatLon(atts));
                    } else if (qName.equals("rte")) {
                        states.push(currentState);
                        currentState = state.rte;
                        currentRoute = new GpxRoute();
                    } else if (qName.equals("trk")) {
                        states.push(currentState);
                        currentState = state.trk;
                        currentTrack = new GpxTrack();
                    } else if (qName.equals("extensions")) {
                        states.push(currentState);
                        currentState = state.ext;
                    } else if (qName.equals("gpx") && atts.getValue("creator").startsWith("Nokia Sports Tracker")) {
                        nokiaSportsTrackerBug = true;
                    }
                    break;
                case author:
                    if (qName.equals("link")) {
                        states.push(currentState);
                        currentState = state.link;
                        currentLink = new GpxLink(atts.getValue("href"));
                    } else if (qName.equals("email")) {
                        currentData.attr.put(GpxData.META_AUTHOR_EMAIL, atts.getValue("id") + "@" + atts.getValue("domain"));
                    }
                    break;
                case trk:
                    if (qName.equals("trkseg")) {
                        states.push(currentState);
                        currentState = state.trkseg;
                        currentTrackSeg = new ArrayList<WayPoint>();
                    } else if (qName.equals("link")) {
                        states.push(currentState);
                        currentState = state.link;
                        currentLink = new GpxLink(atts.getValue("href"));
                    } else if (qName.equals("extensions")) {
                        states.push(currentState);
                        currentState = state.ext;
                    }
                    break;
                case metadata:
                    if (qName.equals("author")) {
                        states.push(currentState);
                        currentState = state.author;
                    } else if (qName.equals("extensions")) {
                        states.push(currentState);
                        currentState = state.ext;
                    } else if (qName.equals("copyright")) {
                        states.push(currentState);
                        currentState = state.copyright;
                        currentData.attr.put(GpxData.META_COPYRIGHT_AUTHOR, atts.getValue("author"));
                    } else if (qName.equals("link")) {
                        states.push(currentState);
                        currentState = state.link;
                        currentLink = new GpxLink(atts.getValue("href"));
                    }
                    break;
                case trkseg:
                    if (qName.equals("trkpt")) {
                        states.push(currentState);
                        currentState = state.wpt;
                        currentWayPoint = new WayPoint(parseLatLon(atts));
                    }
                    break;
                case wpt:
                    if (qName.equals("link")) {
                        states.push(currentState);
                        currentState = state.link;
                        currentLink = new GpxLink(atts.getValue("href"));
                    } else if (qName.equals("extensions")) {
                        states.push(currentState);
                        currentState = state.ext;
                    }
                    break;
                case rte:
                    if (qName.equals("link")) {
                        states.push(currentState);
                        currentState = state.link;
                        currentLink = new GpxLink(atts.getValue("href"));
                    } else if (qName.equals("rtept")) {
                        states.push(currentState);
                        currentState = state.wpt;
                        currentWayPoint = new WayPoint(parseLatLon(atts));
                    } else if (qName.equals("extensions")) {
                        states.push(currentState);
                        currentState = state.ext;
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
                case trk: return currentTrack.attr;
                default: return null;
            }
        }

        @Override public void endElement(String namespaceURI, String qName, String rqName) {
            switch (currentState) {
                case metadata:
                    if (qName.equals("name")) {
                        currentData.attr.put(GpxData.META_NAME, accumulator.toString());
                    } else if (qName.equals("desc")) {
                        currentData.attr.put(GpxData.META_DESC, accumulator.toString());
                    } else if (qName.equals("time")) {
                        currentData.attr.put(GpxData.META_TIME, accumulator.toString());
                    } else if (qName.equals("keywords")) {
                        currentData.attr.put(GpxData.META_KEYWORDS, accumulator.toString());
                    } else if (qName.equals("metadata")) {
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
                    if (currentState == state.author) {
                        currentData.attr.put(GpxData.META_AUTHOR_LINK, currentLink);
                    } else if (currentState != state.link) {
                        Map<String, Object> attr = getAttr();
                        if (!attr.containsKey(GpxData.META_LINKS)) {
                            attr.put(GpxData.META_LINKS, new LinkedList<GpxLink>());
                        }
                        ((Collection<GpxLink>) attr.get(GpxData.META_LINKS)).add(currentLink);
                    }
                    break;
                case wpt:
                    if (   qName.equals("ele")  || qName.equals("magvar")
                            || qName.equals("name") || qName.equals("geoidheight")
                            || qName.equals("type") || qName.equals("sym")) {
                        currentWayPoint.attr.put(qName, accumulator.toString());
                    } else if(qName.equals("hdop") /*|| qName.equals("vdop") ||
                        qName.equals("pdop")*/) {
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
                        currentRoute.routePoints.add(currentWayPoint);
                    } else if (qName.equals("trkpt")) {
                        currentState = states.pop();
                        currentTrackSeg.add(currentWayPoint);
                    } else if (qName.equals("wpt")) {
                        currentState = states.pop();
                        currentData.waypoints.add(currentWayPoint);
                    }
                    break;
                case trkseg:
                    if (qName.equals("trkseg")) {
                        currentState = states.pop();
                        currentTrack.trackSegs.add(currentTrackSeg);
                    }
                    break;
                case trk:
                    if (qName.equals("trk")) {
                        currentState = states.pop();
                        currentData.tracks.add(currentTrack);
                    } else if (qName.equals("name") || qName.equals("cmt")
                            || qName.equals("desc") || qName.equals("src")
                            || qName.equals("type") || qName.equals("number")
                            || qName.equals("url")) {
                        currentTrack.attr.put(qName, accumulator.toString());
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
                        currentData.routes.add(currentRoute);
                    }
            }
        }

        @Override public void endDocument() throws SAXException  {
            if (!states.empty())
                throw new SAXException(tr("Parse error: invalid document structure for GPX document."));
            data = currentData;
        }
    }

    /**
     * Parse the input stream and store the result in trackData and markerData
     *
     * @param relativeMarkerPath The directory to use as relative path for all &lt;wpt&gt;
     *    marker tags. Maybe <code>null</code>, in which case no relative urls are constructed for the markers.
     */
    public GpxReader(InputStream source, File relativeMarkerPath) throws SAXException, IOException {

        Parser parser = new Parser();
        InputSource inputSource = new InputSource(new InputStreamReader(source, "UTF-8"));
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.newSAXParser().parse(inputSource, parser);
        } catch (ParserConfigurationException e) {
            e.printStackTrace(); // broken SAXException chaining
            throw new SAXException(e);
        }
    }
}
