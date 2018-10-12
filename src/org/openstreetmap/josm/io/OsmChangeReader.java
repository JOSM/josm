// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.util.Arrays;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Pair;

/**
 * Reader for <a href="http://wiki.openstreetmap.org/wiki/OsmChange">OsmChange</a> file format.
 */
public class OsmChangeReader extends OsmReader {

    /**
     * List of possible actions.
     */
    private static final String[] ACTIONS = {"create", "modify", "delete"};

    protected final NoteData noteData = new NoteData();

    /**
     * constructor (for private and subclasses use only)
     *
     * @see #parseDataSet(InputStream, ProgressMonitor)
     */
    protected OsmChangeReader() {
        // Restricts visibility
    }

    @Override
    protected void parseRoot() throws XMLStreamException {
        if ("osmChange".equals(parser.getLocalName())) {
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
        if (!"0.6".equals(v)) {
            throwException(tr("Unsupported version: {0}", v));
        }
        ds.setVersion(v);
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (Arrays.asList(ACTIONS).contains(parser.getLocalName())) {
                    parseCommon(parser.getLocalName());
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void parseCommon(String action) throws XMLStreamException {
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                OsmPrimitive p = null;
                switch (parser.getLocalName()) {
                case "node":
                    p = parseNode();
                    break;
                case "way":
                    p = parseWay();
                    break;
                case "relation":
                    p = parseRelation();
                    break;
                case "note":
                    parseNote();
                    break;
                default:
                    parseUnknown();
                }
                if (p != null && action != null) {
                    if ("modify".equals(action)) {
                        p.setModified(true);
                    } else if ("delete".equals(action)) {
                        p.setDeleted(true);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void parseNote() throws XMLStreamException {
        LatLon location = NoteReader.parseLatLon(s -> parser.getAttributeValue(null, s));
        String text = null;
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (parser.getLocalName()) {
                case "comment":
                    text = parser.getAttributeValue(null, "text");
                    jumpToEnd();
                    break;
                default:
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        if (location != null && text != null && !text.isEmpty()) {
            noteData.createNote(location, text);
        }
    }

    /**
     * Replies the parsed notes data.
     * @return the parsed notes data
     * @since 14101
     */
    public final NoteData getNoteData() {
        return noteData;
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source the source input stream. Must not be <code>null</code>.
     * @param progressMonitor  the progress monitor. If <code>null</code>,
     * {@link org.openstreetmap.josm.gui.progress.NullProgressMonitor#INSTANCE} is assumed
     *
     * @return the dataset with the parsed data
     * @throws IllegalDataException if the an error was found while parsing the data from the source
     * @throws IllegalArgumentException if source is <code>null</code>
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new OsmChangeReader().doParseDataSet(source, progressMonitor);
    }

    /**
     * Parse the given input source and return the dataset and notes, if any (OsmAnd extends the osmChange format by adding notes).
     *
     * @param source the source input stream. Must not be <code>null</code>.
     * @param progressMonitor  the progress monitor. If <code>null</code>,
     * {@link org.openstreetmap.josm.gui.progress.NullProgressMonitor#INSTANCE} is assumed
     *
     * @return the dataset with the parsed data
     * @throws IllegalDataException if the an error was found while parsing the data from the source
     * @throws IllegalArgumentException if source is <code>null</code>
     * @since 14101
     */
    public static Pair<DataSet, NoteData> parseDataSetAndNotes(InputStream source, ProgressMonitor progressMonitor)
            throws IllegalDataException {
        OsmChangeReader osmChangeReader = new OsmChangeReader();
        osmChangeReader.doParseDataSet(source, progressMonitor);
        return new Pair<>(osmChangeReader.getDataSet(), osmChangeReader.getNoteData());
    }
}
