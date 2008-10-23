//License: GPL. Copyright 2008 by Christoph Brill

package org.openstreetmap.josm.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Read a nmea file. Based on information from
 * http://www.kowoma.de/gps/zusatzerklaerungen/NMEA.htm
 * 
 * @author cbrill
 */
public class NmeaReader {

	/** Handler for the different types that NMEA speaks. */
	public static enum NMEA_TYPE {

		/** RMC = recommended minimum sentence C. */
		GPRMC("$GPRMC"),
		/** GPS positions. */
		GPGGA("$GPGGA"),
		/** SA = satellites active. */
		GPGSA("$GPGSA");

		private final String type;

		NMEA_TYPE(String type) {
			this.type = type;
		}

		public String getType() {
			return this.type;
		}

		public boolean equals(String type) {
			return this.type.equals(type);
		}
	}

	private static final int TYPE = 0;

	// The following only applies to GPRMC
	public static enum GPRMC {
		TIME(1),
		/** Warning from the receiver (A = data ok, V = warning) */
		RECEIVER_WARNING(2), 
		WIDTH_NORTH(3), WIDTH_NORTH_NAME(4), 
		LENGTH_EAST(5), LENGTH_EAST_NAME(6),
		/** Speed in knots */
		SPEED(7), COURSE(8), DATE(9),
		/** magnetic declination */
		MAGNETIC_DECLINATION(10), UNKNOWN(11),
		/**
		 * Mode (A = autonom; D = differential; E = estimated; N = not valid; S
		 * = simulated)
		 * 
		 * @since NMEA 2.3
		 */
		MODE(12);

		public final int position;

		GPRMC(int position) {
			this.position = position;
		}
	}

	// The following only applies to GPGGA
	public static enum GPGGA {
		TIME(1), LATITUDE(2), LATITUDE_NAME(3), LONGITUDE(4), LONGITUDE_NAME(5),
		/**
		 * Quality (0 = invalid, 1 = GPS, 2 = DGPS, 6 = estimanted (@since NMEA
		 * 2.3))
		 */
		QUALITY(6), SATELLITE_COUNT(7),
		/** HDOP (horizontal dilution of precision) */
		HDOP(8),
		/** height above NN (above geoid) */
		HEIGHT(9), HEIGHT_UNTIS(10),
		/** height geoid - height ellipsoid (WGS84) */
		HEIGHT_2(11), HEIGHT_2_UNTIS(12);

		public final int position;

		GPGGA(int position) {
			this.position = position;
		}
	}

	// The following only applies to GPGGA
	public static enum GPGSA {
		AUTOMATIC(1),
		/** 1 = not fixed, 2 = 2D fixed, 3 = 3D fixed) */
		FIX_TYPE(2),
		// PRN numbers for max 12 satellites
		PRN_1(3), PRN_2(4), PRN_3(5), PRN_4(6), PRN_5(7), PRN_6(8), PRN_7(9), PRN_8(
		        10), PRN_9(11), PRN_10(12), PRN_11(13), PRN_12(14),
		/** PDOP (precision) */
		PDOP(15),
		/** HDOP (horizontal precision) */
		HDOP(16),
		/** VDOP (vertical precision) */
		VDOP(17), ;

		public final int position;

		GPGSA(int position) {
			this.position = position;
		}
	}

	public GpxData data;

	public NmeaReader(InputStream source, File relativeMarkerPath) {
		data = new GpxData();
		GpxTrack currentTrack = new GpxTrack();
		Collection<WayPoint> currentTrackSeg = new ArrayList<WayPoint>();
		currentTrack.trackSegs.add(currentTrackSeg);
		data.tracks.add(currentTrack);

		BufferedReader rd;
		String nmeaWithChecksum;

		try {
			rd = new BufferedReader(new InputStreamReader(source));
			while ((nmeaWithChecksum = rd.readLine()) != null) {
				String[] nmeaAndChecksum = nmeaWithChecksum.split("\\*");
				String nmea = nmeaAndChecksum[0];
				// XXX: No need for it: String checksum = nmeaAndChecksum[1];
				String[] e = nmea.split(",");
				if (e.length == 0) {
				    continue;
				}
				if (NMEA_TYPE.GPRMC.equals(e[TYPE])) {
					LatLon latLon = parseLatLon(e);
					if (latLon == null) {
						continue;
					}
					WayPoint currentWayPoint = new WayPoint(latLon);
					currentTrackSeg.add(currentWayPoint);
				}
			}
			rd.close();
		} catch (final IOException e) {
			System.out.println("Error reading file");
		}

	}

	private LatLon parseLatLon(String[] e) throws NumberFormatException {
	    // If the array looks bogus don't try to get valuable information from it
	    // But remember that the array is stripped of checksum and GPRMC is only 12 elements and split strips empty trailing elements  
	    if (e.length < 10) {
	        return null;
	    }
        String widthNorth = e[GPRMC.WIDTH_NORTH.position].trim();
		String lengthEast = e[GPRMC.LENGTH_EAST.position].trim();
		if ("".equals(widthNorth) || "".equals(lengthEast)) {
			return null;
		}

		// The format is xxDDLL.LLLL
		// xx optional whitespace
		// DD (int) degres
		// LL.LLLL (double) latidude
		int latdegsep = widthNorth.indexOf('.') - 2;
		if (latdegsep < 0) {
			return null;
		}
		int latdeg = Integer.parseInt(widthNorth.substring(0, latdegsep));
		double latmin = Double.parseDouble(widthNorth.substring(latdegsep));
		double lat = latdeg + latmin / 60;
		if ("S".equals(e[GPRMC.WIDTH_NORTH_NAME.position])) {
			lat = -lat;
		}	

		int londegsep = lengthEast.indexOf('.') - 2;
		if (londegsep < 0) {
			return null;
		}
		int londeg = Integer.parseInt(lengthEast.substring(0, londegsep));
		double lonmin = Double.parseDouble(lengthEast.substring(londegsep));
		double lon = londeg + lonmin / 60;
		if ("W".equals(e[GPRMC.LENGTH_EAST_NAME.position])) {
			lon = -lon;
		}
		
		return new LatLon(lat, lon);
	}
}
