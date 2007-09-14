// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.xml.sax.SAXException;


public class BoundingBoxDownloader extends OsmServerReader {

	/**
     * The boundings of the desired map data.
     */
    private final double lat1;
	private final double lon1;
	private final double lat2;
	private final double lon2;

	public BoundingBoxDownloader(double lat1, double lon1, double lat2, double lon2) {
		this.lat1 = lat1;
		this.lon1 = lon1;
		this.lat2 = lat2;
		this.lon2 = lon2;
    }

	/**
     * Retrieve raw gps waypoints from the server API.
     * @return A list of all primitives retrieved. Currently, the list of lists
     * 		contain only one list, since the server cannot distinguish between
     * 		ways.
     */
    public Collection<Collection<GpsPoint>> parseRawGps() throws IOException, SAXException {
		Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
    	try {
    		String url = "trackpoints?bbox="+lon1+","+lat1+","+lon2+","+lat2+"&page=";
    		Collection<Collection<GpsPoint>> data = new LinkedList<Collection<GpsPoint>>();
    		Collection<GpsPoint> list = new LinkedList<GpsPoint>();

    		for (int i = 0;;++i) {
    			Main.pleaseWaitDlg.currentAction.setText(tr("Downloading points {0} to {1}...", i * 5000, ((i + 1) * 5000)));
    			InputStream in = getInputStream(url+i, Main.pleaseWaitDlg);
    			if (in == null)
    				break;
    			// Use only track points, since the server mix everything together 
    			Collection<Collection<GpsPoint>> allWays = new RawGpsReader(in, null).trackData;

    			boolean foundSomething = false;
    			for (Collection<GpsPoint> t : allWays) {
    				if (!t.isEmpty()) {
    					foundSomething = true;
    					list.addAll(t);
    				}
    			}
    			if (!foundSomething)
    				break;
    			in.close();
    			activeConnection = null;
    		}
    		if (!list.isEmpty())
    			data.add(list);
    		return data;
    	} catch (IllegalArgumentException e) {
    		// caused by HttpUrlConnection in case of illegal stuff in the response
    		if (cancel)
    			return null;
    		throw new SAXException("Illegal characters within the HTTP-header response", e);
    	} catch (IOException e) {
    		if (cancel)
    			return null;
    		throw e;
    	} catch (SAXException e) {
    		throw e;
    	} catch (Exception e) {
    		if (cancel)
    			return null;
    		if (e instanceof RuntimeException)
    			throw (RuntimeException)e;
    		throw new RuntimeException(e);
    	}
    }

	/**
     * Read the data from the osm server address.
     * @return A data set containing all data retrieved from that url
     */
    public DataSet parseOsm() throws SAXException, IOException {
    	try {
    		Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
    		final InputStream in = getInputStream("map?bbox="+lon1+","+lat1+","+lon2+","+lat2, Main.pleaseWaitDlg);
    		if (in == null)
    			return null;
    		Main.pleaseWaitDlg.currentAction.setText(tr("Downloading OSM data..."));
    		final DataSet data = OsmReader.parseDataSet(in, null, Main.pleaseWaitDlg);
    		String origin = Main.pref.get("osm-server.url")+"/"+Main.pref.get("osm-server.version", "0.4");
    		Bounds bounds = new Bounds(new LatLon(lat1, lon1), new LatLon(lat2, lon2));
			DataSource src = new DataSource(bounds, origin);
    		data.dataSources.add(src);
    		in.close();
    		activeConnection = null;
    		return data;
    	} catch (IOException e) {
    		if (cancel)
    			return null;
    		throw e;
    	} catch (SAXException e) {
    		throw e;
    	} catch (Exception e) {
    		if (cancel)
    			return null;
    		if (e instanceof RuntimeException)
    			throw (RuntimeException)e;
    		throw new RuntimeException(e);
    	}
    }
}
