// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Writes GPX files from GPX data or OSM data.
 */
public class GpxWriter extends XmlWriter {

	public GpxWriter(PrintWriter out) {
		super(out);
	}

	public GpxWriter(OutputStream out) {
		super(new PrintWriter(out));
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
		out.println("<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\">");
		indent = "  ";
		writeMetaData();
		writeWayPoints();
		writeRoutes();
		writeTracks();
		out.print("</gpx>");
		out.flush();
		out.close();
	}

	private void writeAttr(Map<String, Object> attr) {
		boolean hasAuthor = false;
		for (Map.Entry<String, Object> ent : attr.entrySet()) {
			String k = ent.getKey();
			if (k.indexOf("author") == 0) {
				hasAuthor = true;
			} else if (k.equals("link")) {
				for (GpxLink link : (Collection<GpxLink>) ent.getValue()) {
					gpxLink(link);
				}
			} else {
				simpleTag(k, (String) ent.getValue());
			}
		}

		if (hasAuthor) {
			open("author");
			simpleTag("name", (String) attr.get("authorname"));
			simpleTag("email", (String) attr.get("authoremail"));
			gpxLink((GpxLink) attr.get("authorlink"));
			closeln("author");
		}

		// TODO: copyright
	}

	private void writeMetaData() {
		openln("metadata");
		writeAttr(data.attr);

		data.recalculateBounds();
		Bounds bounds = data.bounds;
		String b = "minlat=\"" + bounds.min.lat() + "\" minlon=\"" + bounds.min.lon() +
			"\" maxlat=\"" + bounds.max.lat() + "\" maxlon=\"" + bounds.max.lon() + "\"" ;
		inline("bounds", b);

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
		out.print("\n");
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
		out.print("\n");
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
			throw new RuntimeException("Bug detected. Please report this!");
		}
		if (pnt != null) {
			openAtt(type, "lat=\"" + pnt.latlon.lat() + "\" lon=\"" + pnt.latlon.lon() + "\"");
			writeAttr(pnt.attr);
			closeln(type);
		}
	}
}
