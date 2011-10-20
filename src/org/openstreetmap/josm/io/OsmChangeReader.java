package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class OsmChangeReader extends OsmReader {

    /**
     * constructor (for private and subclasses use only)
     *
     * @see #parseDataSet(InputStream, DataSet, ProgressMonitor)
     */
    protected OsmChangeReader() {
    }
    
    /* (non-Javadoc)
     * @see org.openstreetmap.josm.io.OsmReader#parseRoot()
     */
    @Override
    protected void parseRoot() throws XMLStreamException {
        if (parser.getLocalName().equals("osmChange")) {
            parseOsmChange();
        } else {
            parseUnknown();
        }
    }

    private void parseOsmChange() throws XMLStreamException {
        String v = parser.getAttributeValue(null, "version");
        if (v == null) {
            throwException(tr("Missing mandatory attribute ''{0}''.", "version"));
        }
        if (!v.equals("0.6")) {
            throwException(tr("Unsupported version: {0}", v));
        }
        ds.setVersion(v);
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("create")) {
                    parseCreate();
                } else if (parser.getLocalName().equals("modify")) {
                    parseModify();
                } else if (parser.getLocalName().equals("delete")) {
                    parseDelete();
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void parseDelete() throws XMLStreamException {
        // Do nothing. If the object has been deleted, do not load it to avoid consistency errors.
        parseCommon(false);
    }

    private void parseModify() throws XMLStreamException {
        parseCommon(true);
    }

    private void parseCreate() throws XMLStreamException {
        parseCommon(true);
    }

    private void parseCommon(boolean keepPrimitive) throws XMLStreamException {
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("node")) {
                    if (keepPrimitive) parseNode(); else doNothing();
                } else if (parser.getLocalName().equals("way")) {
                    if (keepPrimitive) parseWay(); else doNothing();
                } else if (parser.getLocalName().equals("relation")) {
                    if (keepPrimitive) parseRelation(); else doNothing();
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }
    
    private void doNothing() throws XMLStreamException {
        while (parser.hasNext() && parser.next() != XMLStreamConstants.END_ELEMENT);
    }
    
    /**
     * Parse the given input source and return the dataset.
     *
     * @param source the source input stream. Must not be null.
     * @param progressMonitor  the progress monitor. If null, {@see NullProgressMonitor#INSTANCE} is assumed
     *
     * @return the dataset with the parsed data
     * @throws IllegalDataException thrown if the an error was found while parsing the data from the source
     * @throws IllegalArgumentException thrown if source is null
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new OsmChangeReader().doParseDataSet(source, progressMonitor);
    }
}
