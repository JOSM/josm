// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerProducers;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Read raw gps data from a gpx file. Only way points with their ways segments
 * and waypoints are imported.
 * @author imi
 */
public class RawGpsReader {

	/**
	 * The relative path when constructing markers from wpt-tags. Passed to 
	 * {@link MarkerProducers#createMarker(LatLon, java.util.Map, String)}
	 */
	private File relativeMarkerPath;

	/**
	 * Hold the resulting gps data (tracks and their track points)
	 */
	public Collection<Collection<GpsPoint>> trackData = new LinkedList<Collection<GpsPoint>>();

	/**
	 * Hold the waypoints of the gps data.
	 */
	public Collection<Marker> markerData = new ArrayList<Marker>();

	private class Parser extends DefaultHandler {
		/**
		 * Current track to be read. The last entry is the current trkpt.
		 * If in wpt-mode, it contain only one GpsPoint.
		 */
		private Collection<GpsPoint> current = new LinkedList<GpsPoint>();
		private LatLon currentLatLon;
		private HashMap<String, String> currentTagValues = new HashMap<String, String>();
		private Stack<String> tags = new Stack<String>();

		@Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			if (qName.equals("wpt") || qName.equals("trkpt")) {
				try {
					double lat = Double.parseDouble(atts.getValue("lat"));
					double lon = Double.parseDouble(atts.getValue("lon"));
					if (Math.abs(lat) > 90)
						throw new SAXException(tr("Data error: lat value \"{0}\" is out of bounds.", lat));
					if (Math.abs(lon) > 180)
						throw new SAXException(tr("Data error: lon value \"{0}\" is out of bounds.", lon));
					currentLatLon = new LatLon(lat, lon);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					throw new SAXException(e);
				}
				currentTagValues.clear();
			}
			tags.push(qName);
		}

		@Override public void characters(char[] ch, int start, int length) {
			String peek = tags.peek();
			if (peek.equals("time") || peek.equals("name") || peek.equals("link") || peek.equals("symbol")) {
				String tag = tags.pop();
				if (tags.empty() || (!tags.peek().equals("wpt") && !tags.peek().equals("trkpt"))) {
					tags.push(tag);
					return;
				}
				String contents = new String(ch, start, length);
				String oldContents = currentTagValues.get(peek);
				if (oldContents == null) {
					currentTagValues.put(peek, contents);
				} else {
					currentTagValues.put(peek, oldContents + contents);	
				}
				tags.push(tag);
			} else if (peek.equals("text")) {
                            String tag = tags.pop();
                            if (tags.empty() || !tags.peek().equals("link")) {
                                tags.push(tag);
                                return;
                            }
                            String contents = new String(ch, start, length);
                            // we just want the contents of <link><text></text></link> to
                            // all be stored under link.
                            String oldContents = currentTagValues.get("link");
                            if (oldContents == null) {
                                currentTagValues.put("link", contents);
                            } else {
                                currentTagValues.put("link", oldContents + contents);
                            }
                            tags.push(tag);
                        }
		}

		@Override public void endElement(String namespaceURI, String localName, String qName) {
			if (qName.equals("trkpt")) {
				current.add(new GpsPoint(currentLatLon, currentTagValues.get("time")));
				currentTagValues.clear();
			} else if (qName.equals("wpt")) {
				Marker m = Marker.createMarker(currentLatLon, currentTagValues, relativeMarkerPath);
				if (m != null)
					markerData.add(m);
				currentTagValues.clear();
			} else if (qName.equals("trkseg") || qName.equals("trk") || qName.equals("gpx")) {
				newTrack();
				currentTagValues.clear();
			} else if (qName.equals("link")) {
                            String contents = currentTagValues.get(qName);
                            if (contents != null) {
                                // strip off leading and trailing whitespace
                                currentTagValues.put(qName,
                                                     contents
                                                      .replaceFirst("^\\s+", "")
                                                      .replaceFirst("\\s+$", ""));
                            }
                        }
			tags.pop();
		}

		private void newTrack() {
			if (!current.isEmpty()) {
				trackData.add(current);
				current = new LinkedList<GpsPoint>();
			}
		}
	}

	/**
	 * Parse the input stream and store the result in trackData and markerData
	 * 
	 * @param relativeMarkerPath The directory to use as relative path for all &lt;wpt&gt; 
	 *    marker tags. Maybe <code>null</code>, in which case no relative urls are constructed for the markers. 
	 */
	public RawGpsReader(InputStream source, File relativeMarkerPath) throws SAXException, IOException {
		this.relativeMarkerPath = relativeMarkerPath;
		Parser parser = new Parser();
		InputSource inputSource = new InputSource(new InputStreamReader(source, "UTF-8"));
		try {
			SAXParserFactory.newInstance().newSAXParser().parse(inputSource, parser);
        } catch (ParserConfigurationException e) {
        	e.printStackTrace(); // broken SAXException chaining
        	throw new SAXException(e);
        }
	}
}
