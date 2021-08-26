// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.ozi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.IGpxReader;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Reads an OziExplorer Waypoint file. Based on information from
 * <a href="https://www.oziexplorer4.com/eng/help/fileformats.html">https://www.oziexplorer4.com</a>.
 * @since 18179
 */
public class OziWptReader implements IGpxReader {

    private static final int IDX_NAME = 1;
    private static final int IDX_LAT = 2;
    private static final int IDX_LON = 3;
    private static final int IDX_DESC = 10;
    private static final int IDX_ELE = 14;

    private static final int INVALID_ELE = -777;

    private final InputStream source;
    private GpxData data;
    private int success; // number of successfully parsed lines

    /**
     * Constructs a new {@code OziWptReader}
     * @param source Ozi wpt file input stream
     * @throws IOException if an I/O error occurs
     */
    public OziWptReader(InputStream source) throws IOException {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public boolean parse(boolean tryToFinish) throws SAXException, IOException {
        data = new GpxData();
        Collection<Collection<WayPoint>> currentTrack = new ArrayList<>();
        Collection<WayPoint> waypoints = new ArrayList<>();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            int linecount = 0;
            String line;
            do {
                line = rd.readLine();
                if (line != null) {
                    linecount++;
                    if (linecount == 1) {
                        if (!line.startsWith("OziExplorer Waypoint File")) {
                            throw new UnsupportedOperationException("Unsupported format: " + line);
                        }
                    } else if (linecount == 2) {
                        if (!"WGS 84".equals(line)) {
                            throw new UnsupportedOperationException("Unsupported datum: " + line);
                        }
                    } else if (linecount == 3 || linecount == 4) {
                        Logging.trace(line);
                    } else {
                        try {
                            String[] fields = line.split(",");
                            WayPoint currentwp = new WayPoint(new LatLon(
                                    Double.parseDouble(fields[IDX_LAT]),
                                    Double.parseDouble(fields[IDX_LON])));
                            currentwp.put(GpxConstants.GPX_NAME, fields[IDX_NAME]);
                            currentwp.put(GpxConstants.GPX_DESC, fields[IDX_DESC]);
                            String ele = fields[IDX_ELE];
                            if (!ele.isEmpty()) {
                                int eleInFeet = Integer.parseInt(ele);
                                if (eleInFeet != 0 && eleInFeet != INVALID_ELE) {
                                    currentwp.put(GpxConstants.PT_ELE, eleInFeet * SystemOfMeasurement.IMPERIAL.aValue);
                                }
                            }
                            waypoints.add(currentwp);
                            success++;
                        } catch (IllegalArgumentException e) {
                            Logging.error(e);
                        }
                    }
                }
            } while (line != null);
        }
        currentTrack.add(waypoints);
        data.tracks.add(new GpxTrack(currentTrack, Collections.<String, Object>emptyMap()));
        return true;
    }

    @Override
    public GpxData getGpxData() {
        return data;
    }

    @Override
    public int getNumberOfCoordinates() {
        return success;
    }
}
