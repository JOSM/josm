// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.rtklib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.IGpxReader;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

/**
 * Reads a RTKLib Positioning Solution file.
 * <p>
 * See <a href="https://github.com/tomojitakasu/RTKLIB/blob/rtklib_2.4.3/doc/manual_2.4.2.pdf">RTKLIB Manual</a>.
 * @since 15247
 */
public class RtkLibPosReader implements IGpxReader {

    private static final int IDX_DATE = 0;
    private static final int IDX_TIME = 1;
    private static final int IDX_LAT = 2;
    private static final int IDX_LON = 3;
    private static final int IDX_HEIGHT = 4;
    private static final int IDX_Q = 5;
    private static final int IDX_NS = 6;
    private static final int IDX_SDN = 7;
    private static final int IDX_SDE = 8;
    private static final int IDX_SDU = 9;
    private static final int IDX_SDNE = 10;
    private static final int IDX_SDEU = 11;
    private static final int IDX_SDUN = 12;
    private static final int IDX_AGE = 13;
    private static final int IDX_RATIO = 14;

    private final SimpleDateFormat dateTimeFmtS = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH); // 2019/06/08 08:23:15
    private final SimpleDateFormat dateTimeFmtL = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.ENGLISH); // 2019/06/08 08:23:15.000

    private final InputStream source;
    private GpxData data;
    private int success; // number of successfully parsed lines

    /**
     * Constructs a new {@code RtkLibPosReader}
     * @param source RTKLib .pos file input stream
     * @throws IOException if an I/O error occurs
     */
    public RtkLibPosReader(InputStream source) throws IOException {
        this.source = Objects.requireNonNull(source);
        dateTimeFmtS.setTimeZone(DateUtils.UTC);
        dateTimeFmtL.setTimeZone(DateUtils.UTC);
    }

    private Date parseDate(String date) throws ParseException {
        return (date.length() > 20 ? dateTimeFmtL : dateTimeFmtS).parse(date);
    }

    @Override
    public boolean parse(boolean tryToFinish) throws SAXException, IOException {
        data = new GpxData();
        Collection<Collection<WayPoint>> currentTrack = new ArrayList<>();
        Collection<WayPoint> waypoints = new ArrayList<>();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            String line = null;
            do {
                line = rd.readLine();
                if (line != null) {
                    if (line.startsWith("% ref pos   :")) {
                        // TODO add marker
                    } else if (!line.startsWith("%")) {
                        try {
                            String[] fields = line.split("[ ]+", -1);
                            WayPoint currentwp = new WayPoint(new LatLon(
                                    Double.parseDouble(fields[IDX_LAT]),
                                    Double.parseDouble(fields[IDX_LON])));
                            currentwp.put(GpxConstants.PT_ELE, fields[IDX_HEIGHT]);
                            currentwp.setTime(parseDate(fields[IDX_DATE]+" "+fields[IDX_TIME]));
                            currentwp.put(GpxConstants.RTKLIB_Q, Integer.parseInt(fields[IDX_Q]));
                            currentwp.put(GpxConstants.PT_SAT, fields[IDX_NS]);
                            currentwp.put(GpxConstants.RTKLIB_SDN, fields[IDX_SDN]);
                            currentwp.put(GpxConstants.RTKLIB_SDE, fields[IDX_SDE]);
                            currentwp.put(GpxConstants.RTKLIB_SDE, fields[IDX_SDU]);
                            currentwp.put(GpxConstants.RTKLIB_SDNE, fields[IDX_SDNE]);
                            currentwp.put(GpxConstants.RTKLIB_SDEU, fields[IDX_SDEU]);
                            currentwp.put(GpxConstants.RTKLIB_SDUN, fields[IDX_SDUN]);
                            currentwp.put(GpxConstants.RTKLIB_AGE, fields[IDX_AGE]);
                            currentwp.put(GpxConstants.RTKLIB_RATIO, fields[IDX_RATIO]);
                            double sdn = Double.parseDouble(fields[IDX_SDN]);
                            double sde = Double.parseDouble(fields[IDX_SDN]);
                            currentwp.put(GpxConstants.PT_HDOP, (float) Math.sqrt(sdn*sdn + sde*sde));
                            waypoints.add(currentwp);
                            success++;
                        } catch (ParseException | IllegalArgumentException e) {
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

    /**
     * Returns the number of coordinates that have been successfuly read.
     * @return the number of coordinates that have been successfuly read
     */
    public int getNumberOfCoordinates() {
        return success;
    }
}
