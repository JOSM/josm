// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.IOException;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.xml.sax.SAXException;

/**
 * Abstraction of {@code GpxReader} and {@code NmeaReader}.
 * @since 14010
 */
public interface IGpxReader {

    /**
     * Parse the GPX data.
     *
     * @param tryToFinish true, if the reader should return at least part of the GPX
     * data in case of an error.
     * @return true if file was properly parsed, false if there was error during
     * parsing but some data were parsed anyway
     * @throws SAXException if any SAX parsing error occurs
     * @throws IOException if any I/O error occurs
     */
    boolean parse(boolean tryToFinish) throws SAXException, IOException;

    /**
     * Replies the GPX data.
     * @return The GPX data
     */
    GpxData getGpxData();
}
