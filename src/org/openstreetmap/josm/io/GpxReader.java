// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Read a gpx file.
 * <p>
 * Bounds are read, even if we calculate them, see {@link GpxData#recalculateBounds}.<br>
 * Both GPX version 1.0 and 1.1 are supported.
 *
 * @author imi, ramack
 */
public class GpxReader implements GpxConstants, IGpxReader {

    /** The resulting gpx data */
    private GpxData gpxData;
    private final InputSource inputSource;

    /**
     * Constructs a new {@code GpxReader}, which can later parse the input stream
     * and store the result in trackData and markerData
     *
     * @param source the source input stream
     * @throws IOException if an IO error occurs, e.g. the input stream is closed.
     */
    public GpxReader(InputStream source) throws IOException {
        Reader utf8stream = UTFInputStreamReader.create(source); // NOPMD
        Reader filtered = new InvalidXmlCharacterFilter(utf8stream); // NOPMD
        this.inputSource = new InputSource(filtered);
    }

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
    @Override
    public boolean parse(boolean tryToFinish) throws SAXException, IOException {
        GpxParser parser = new GpxParser();
        try {
            XmlUtils.parseSafeSAX(inputSource, parser);
            return true;
        } catch (SAXException e) {
            if (tryToFinish) {
                parser.tryToFinish();
                String message = e.getLocalizedMessage();
                if (e instanceof SAXParseException) {
                    boolean dot = message.lastIndexOf('.') == message.length() - 1;
                    if (dot)
                        message = message.substring(0, message.length() - 1);
                    SAXParseException spe = (SAXParseException) e;
                    message += ' ' + tr("(at line {0}, column {1})", spe.getLineNumber(), spe.getColumnNumber());
                    if (dot)
                        message += '.';
                }
                if (!Utils.isBlank(parser.getData().creator)) {
                    message += "\n" + tr("The file was created by \"{0}\".", parser.getData().creator);
                }
                SAXException ex = new SAXException(message, e);
                if (parser.getData().isEmpty())
                    throw ex;
                Logging.warn(ex);
                return false;
            } else
                throw e;
        } catch (ParserConfigurationException e) {
            Logging.error(e); // broken SAXException chaining
            throw new SAXException(e);
        } finally {
            if (parser.getData() != null) {
                this.gpxData = parser.getData();
            }
        }
    }

    @Override
    public GpxData getGpxData() {
        return gpxData;
    }
}
