// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.xml.sax.SAXException;

/**
 * Read raw information from a csv style file (as defined in the preferences).
 * @author imi
 */
public class RawCsvReader {

	/**
	 * Reader to read the input from.
	 */
	private BufferedReader in;

	public RawCsvReader(Reader in) {
		this.in = new BufferedReader(in);
	}

	public Collection<GpsPoint> parse() throws SAXException, IOException {
		Collection<GpsPoint> data = new LinkedList<GpsPoint>();
		String formatStr = Main.pref.get("csv.importstring");
		if (formatStr == null || formatStr.equals(""))
			formatStr = in.readLine();
		if (formatStr == null || formatStr.equals(""))
			throw new SAXException(tr("Could not detect data format string."));

		// get delimiter
		String delim = ",";
		for (int i = 0; i < formatStr.length(); ++i) {
			if (!Character.isLetterOrDigit(formatStr.charAt(i))) {
				delim = ""+formatStr.charAt(i);
				break;
			}
		}

		// convert format string
		ArrayList<String> format = new ArrayList<String>();
		for (StringTokenizer st = new StringTokenizer(formatStr, delim); st.hasMoreTokens();) {
			String token = st.nextToken();
			if (!token.equals("lat") && !token.equals("lon") && !token.equals("time"))
				token = "ignore";
			format.add(token);
		}

		// test for completness
		if (!format.contains("lat") || !format.contains("lon")) {
			if (Main.pref.get("csv.importstring").equals(""))
				throw new SAXException(tr("Format string in data is incomplete or not found. Try setting an manual format string in preferences."));
			throw new SAXException(tr("Format string is incomplete. Need at least 'lat' and 'lon' specification"));
		}

		int lineNo = 0;
		try {
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				lineNo++;
				StringTokenizer st = new StringTokenizer(line, delim);
				double lat = 0, lon = 0;
				String time = null;
				for (String token : format) {
					if (token.equals("lat"))
						lat = Double.parseDouble(st.nextToken());
					else if (token.equals("lon"))
						lon = Double.parseDouble(st.nextToken());
					else if (token.equals("time"))
						time = (time == null?"":(time+" ")) + st.nextToken();
					else if (token.equals("ignore"))
						st.nextToken();
					else
						throw new SAXException(tr("Unknown data type: \"{0}\".",token)+(Main.pref.get("csv.importstring").equals("") ? (" "+tr("Maybe add a format string in preferences.")) : ""));
				}
				data.add(new GpsPoint(new LatLon(lat, lon), time));
			}
		} catch (RuntimeException e) {
			throw new SAXException(tr("Parsing error in line {0}",lineNo), e);
		}
		return data;
	}
}
