// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.nmea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.IGpxReader;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Logging;

/**
 * Reads a NMEA 0183 file. Based on information from
 * <a href="http://www.catb.org/gpsd/NMEA.html">http://www.catb.org/gpsd</a>.
 *
 * NMEA files are in printable ASCII form and may include information such as position,
 * speed, depth, frequency allocation, etc.
 * Typical messages might be 11 to a maximum of 79 characters in length.
 *
 * NMEA standard aims to support one-way serial data transmission from a single "talker"
 * to one or more "listeners". The type of talker is identified by a 2-character mnemonic.
 *
 * NMEA information is encoded through a list of "sentences".
 *
 * @author cbrill
 */
public class NmeaReader implements IGpxReader {

    private final InputStream source;
    GpxData data;

    private NmeaParser ps;

    /* functions for reading the error stats */

    /**
     * Number of unknown sentences
     * @return return the number of unknown sentences encountered
     */
    public int getParserUnknown() {
        return ps.getParserUnknown();
    }

    /**
     * Number of empty coordinates
     * @return return the number of coordinates which have been zero
     */
    public int getParserZeroCoordinates() {
        return ps.zeroCoord;
    }

    /**
     * Number of checksum errors
     * @return return the number of sentences with checksum errors
     */
    public int getParserChecksumErrors() {
        return ps.checksumErrors+ps.noChecksum;
    }

    /**
     * Number of malformed errors
     * @return return the number of malformed sentences
     */
    public int getParserMalformed() {
        return ps.malformed;
    }

    @Override
    public int getNumberOfCoordinates() {
        return ps.getSuccess();
    }

    /**
     * Constructs a new {@code NmeaReader}
     * @param source NMEA file input stream
     * @throws IOException if an I/O error occurs
     */
    public NmeaReader(InputStream source) throws IOException {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public boolean parse(boolean tryToFinish) throws IOException {
        // create the data tree
        data = new GpxData();
        Collection<Collection<WayPoint>> currentTrack = new ArrayList<>();

        try (BufferedReader rd = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(1024);
            int loopstartChar = rd.read();
            ps = new NmeaParser();
            if (loopstartChar == -1)
                //TODO tell user about the problem?
                return false;
            sb.append((char) loopstartChar);
            while (true) {
                // don't load unparsable files completely to memory
                if (sb.length() >= 1020) {
                    sb.delete(0, sb.length()-1);
                }
                int c = rd.read();
                if (c == '$') {
                    ps.parseNMEASentence(sb.toString());
                    sb.delete(0, sb.length());
                    sb.append('$');
                } else if (c == -1) {
                    // EOF: add last WayPoint if it works out
                    ps.parseNMEASentence(sb.toString());
                    break;
                } else {
                    sb.append((char) c);
                }
            }
            currentTrack.add(ps.getWaypoints());
            data.tracks.add(new GpxTrack(currentTrack, Collections.<String, Object>emptyMap()));

        } catch (IllegalDataException e) {
            Logging.warn(e);
            return false;
        }
        return true;
    }

    @Override
    public GpxData getGpxData() {
        return data;
    }
}
