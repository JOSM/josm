// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Parser for OSM history data.
 *
 * It is slightly different from {@link OsmReader} because we don't build an internal graph of
 * {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s. We use objects derived from
 * {@link HistoryOsmPrimitive} instead and we keep the data in a dedicated {@link HistoryDataSet}.
 * @since 1670
 */
public class OsmHistoryReader {

    private final InputStream in;
    private final HistoryDataSet data;

    private class Parser extends AbstractParser {

        protected String getCurrentPosition() {
            if (locator == null)
                return "";
            return new StringBuilder().append('(').append(locator.getLineNumber())
                                      .append(',').append(locator.getColumnNumber()).append(')').toString();
        }

        @Override
        protected void throwException(String message) throws SAXException {
            throw new SAXException(getCurrentPosition() + message);
        }

        @Override
        protected void throwException(String message, Exception e) throws SAXException {
            throw new SAXException(getCurrentPosition() + message, e);
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            doStartElement(qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("node".equals(qName)
                    || "way".equals(qName)
                    || "relation".equals(qName)) {
                data.put(currentPrimitive);
            }
        }
    }

    /**
     * Constructs a new {@code OsmHistoryReader}.
     *
     * @param source the input stream with the history content as XML document. Must not be null.
     * @throws IllegalArgumentException if source is {@code null}.
     */
    public OsmHistoryReader(InputStream source) {
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        this.in = source;
        this.data = new HistoryDataSet();
    }

    /**
     * Parses the content.
     * @param progressMonitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @return the parsed data
     * @throws SAXException If any SAX errors occur during processing.
     * @throws IOException If any IO errors occur.
     */
    public HistoryDataSet parse(ProgressMonitor progressMonitor) throws SAXException, IOException {
        InputSource inputSource = new InputSource(new InputStreamReader(in, StandardCharsets.UTF_8));
        progressMonitor.beginTask(tr("Parsing OSM history data ..."));
        try {
            XmlUtils.parseSafeSAX(inputSource, new Parser());
        } catch (ParserConfigurationException e) {
            Logging.error(e); // broken SAXException chaining
            throw new SAXException(e);
        } finally {
            progressMonitor.finishTask();
        }
        return data;
    }
}
