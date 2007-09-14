// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.testframework.MainMock;
import org.xml.sax.SAXException;

public class BoundingBoxDownloaderTest extends MainMock {

	private class StringDownloader extends BoundingBoxDownloader {
	    ByteArrayInputStream stream = in == null ? null : new ByteArrayInputStream(in.getBytes());
	    private StringDownloader() {super(1,2,3,4);}
	    @Override protected InputStream getInputStream(String urlStr, PleaseWaitDialog pleaseWaitDlg) throws IOException {
	    	ByteArrayInputStream oldStream = stream;
	    	stream = null;
	    	return oldStream;
	    }
    }

	private BoundingBoxDownloader bbox;
	private String in;
	private Collection<Collection<GpsPoint>> gps;
	private Collection<GpsPoint> trk;
	private DataSet ds;

	private void parseRaw() throws IOException, SAXException {
		bbox = new StringDownloader();
		gps = bbox.parseRawGps();
		if (gps != null && gps.size() > 0)
			trk = gps.iterator().next();
	}

	private void parseOsm() throws IOException, SAXException {
		bbox = new StringDownloader();
		ds = bbox.parseOsm();
	}

	
	@Test public void parseRawGpsEmptyDataReturnsEmptyList() throws Exception {
		in = "<gpx></gpx>";
		parseRaw();
		assertEquals(0, gps.size());
	}
	
	@Test public void parseRawGpsOneTrack() throws Exception {
	    in = "<gpx><trk><trkseg><trkpt lat='1' lon='2'/></trkseg></trk></gpx>";
		parseRaw();
		assertEquals(1, gps.size());
		assertEquals(1, trk.size());
		assertEquals(1.0, trk.iterator().next().latlon.lat());
		assertEquals(2.0, trk.iterator().next().latlon.lon());
    }
	
	@Test public void parseRawGpsMultipleTracksReturnStillOneTrack() throws Exception {
	    in = "<gpx>" +
	    		"<trk>" +
	    		"<trkseg><trkpt lat='23' lon='42'/></trkseg>" +
	    		"<trkseg><trkpt lat='12' lon='34'/></trkseg>" +
	    		"</trk>" +
	    		"<trk><trkseg><trkpt lat='3' lon='4'/></trkseg></trk>" +
	    		"</gpx>";
	    parseRaw();
	    assertEquals(1, gps.size());
	    assertEquals(3, trk.size());
		assertEquals(42.0, trk.iterator().next().latlon.lon());
    }

	@Test public void parseOsmReturnNullIfNullInputStream() throws Exception {
	    in = null;
	    parseOsm();
	    assertNull(ds);
    }
	
	@Test public void parseOsmEmpty() throws Exception {
	    in = "<osm version='0.4'></osm>";
	    parseOsm();
	    assertEquals(0, ds.nodes.size());
	    assertEquals(0, ds.segments.size());
	    assertEquals(0, ds.ways.size());
    }
	
	@Test public void parseOsmSimpleNode() throws Exception {
	    in = "<osm version='0.4'><node id='123' lat='12' lon='23'/></osm>";
	    parseOsm();
	    assertEquals(1, ds.nodes.size());
	    Node node = ds.nodes.iterator().next();
		assertEquals(123, node.id);
	    assertEquals(12.0, node.coor.lat());
	    assertEquals(23.0, node.coor.lon());
	    assertNull(node.keys);
    }
	
	@Test public void parseOsmComplexWay() throws Exception {
	    in = "<osm version='0.4'>" +
	    		"<way id='1'>" +
	    		"<seg id='2' />" +
	    		"</way>" +
	    		"<segment id='2' from='3' to='3' />" +
	    		"<node id='3' lat='1' lon='2'><tag k='foo' v='bar' /></node>" +
	    		"</osm>";
	    
	    parseOsm();
	    
	    assertEquals(1, ds.nodes.size());
	    assertEquals(1, ds.segments.size());
	    assertEquals(1, ds.ways.size());
	    
	    Node node = ds.nodes.iterator().next();
	    Segment segment = ds.segments.iterator().next();
	    Way way = ds.ways.iterator().next();

	    assertNotNull(node.keys);
	    assertEquals("bar", node.get("foo"));
	    assertEquals(1.0, node.coor.lat());
		assertEquals(node, segment.from);
		assertEquals(1, way.segments.size());
		assertEquals(segment, way.segments.iterator().next());
    }
}
