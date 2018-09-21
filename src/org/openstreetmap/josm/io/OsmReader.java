// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.XmlUtils;

/**
 * Parser for the Osm API (XML output). Read from an input stream and construct a dataset out of it.
 *
 * For each xml element, there is a dedicated method.
 * The XMLStreamReader cursor points to the start of the element, when the method is
 * entered, and it must point to the end of the same element, when it is exited.
 */
public class OsmReader extends AbstractReader {

    protected XMLStreamReader parser;

    /**
     * constructor (for private and subclasses use only)
     *
     * @see #parseDataSet(InputStream, ProgressMonitor)
     */
    protected OsmReader() {
        // Restricts visibility
    }

    protected void setParser(XMLStreamReader parser) {
        this.parser = parser;
    }

    protected void throwException(Throwable th) throws XMLStreamException {
        throw new XmlStreamParsingException(th.getMessage(), parser.getLocation(), th);
    }

    protected void throwException(String msg, Throwable th) throws XMLStreamException {
        throw new XmlStreamParsingException(msg, parser.getLocation(), th);
    }

    protected void throwException(String msg) throws XMLStreamException {
        throw new XmlStreamParsingException(msg, parser.getLocation());
    }

    protected void parse() throws XMLStreamException {
        int event = parser.getEventType();
        while (true) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                parseRoot();
            } else if (event == XMLStreamConstants.END_ELEMENT)
                return;
            if (parser.hasNext()) {
                event = parser.next();
            } else {
                break;
            }
        }
        parser.close();
    }

    protected void parseRoot() throws XMLStreamException {
        if ("osm".equals(parser.getLocalName())) {
            parseOsm();
        } else {
            parseUnknown();
        }
    }

    private void parseOsm() throws XMLStreamException {
        try {
            parseVersion(parser.getAttributeValue(null, "version"));
            parseDownloadPolicy("download", parser.getAttributeValue(null, "download"));
            parseUploadPolicy("upload", parser.getAttributeValue(null, "upload"));
            parseLocked(parser.getAttributeValue(null, "locked"));
        } catch (IllegalDataException e) {
            throwException(e);
        }
        String generator = parser.getAttributeValue(null, "generator");
        Long uploadChangesetId = null;
        if (parser.getAttributeValue(null, "upload-changeset") != null) {
            uploadChangesetId = getLong("upload-changeset");
        }
        while (parser.hasNext()) {
            int event = parser.next();

            if (cancel) {
                cancel = false;
                throw new OsmParsingCanceledException(tr("Reading was canceled"), parser.getLocation());
            }

            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (parser.getLocalName()) {
                case "bounds":
                    parseBounds(generator);
                    break;
                case "node":
                    parseNode();
                    break;
                case "way":
                    parseWay();
                    break;
                case "relation":
                    parseRelation();
                    break;
                case "changeset":
                    parseChangeset(uploadChangesetId);
                    break;
                case "remark": // Used by Overpass API
                    parseRemark();
                    break;
                default:
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void handleIllegalDataException(IllegalDataException e) throws XMLStreamException {
        Throwable cause = e.getCause();
        if (cause instanceof XMLStreamException) {
            throw (XMLStreamException) cause;
        } else {
            throwException(e);
        }
    }

    private void parseRemark() throws XMLStreamException {
        while (parser.hasNext()) {
            int event = parser.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                ds.setRemark(parser.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void parseBounds(String generator) throws XMLStreamException {
        String minlon = parser.getAttributeValue(null, "minlon");
        String minlat = parser.getAttributeValue(null, "minlat");
        String maxlon = parser.getAttributeValue(null, "maxlon");
        String maxlat = parser.getAttributeValue(null, "maxlat");
        String origin = parser.getAttributeValue(null, "origin");
        try {
            parseBounds(generator, minlon, minlat, maxlon, maxlat, origin);
        } catch (IllegalDataException e) {
            handleIllegalDataException(e);
        }
        jumpToEnd();
    }

    protected Node parseNode() throws XMLStreamException {
        String lat = parser.getAttributeValue(null, "lat");
        String lon = parser.getAttributeValue(null, "lon");
        try {
            return parseNode(lat, lon, this::readCommon, this::parseNodeTags);
        } catch (IllegalDataException e) {
            handleIllegalDataException(e);
        }
        return null;
    }

    private void parseNodeTags(Node n) throws IllegalDataException {
        try {
            while (parser.hasNext()) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if ("tag".equals(parser.getLocalName())) {
                        parseTag(n);
                    } else {
                        parseUnknown();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    return;
                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalDataException(e);
        }
    }

    protected Way parseWay() throws XMLStreamException {
        try {
            return parseWay(this::readCommon, this::parseWayNodesAndTags);
        } catch (IllegalDataException e) {
            handleIllegalDataException(e);
        }
        return null;
    }

    private void parseWayNodesAndTags(Way w, Collection<Long> nodeIds) throws IllegalDataException {
        try {
            while (parser.hasNext()) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (parser.getLocalName()) {
                    case "nd":
                        nodeIds.add(parseWayNode(w));
                        break;
                    case "tag":
                        parseTag(w);
                        break;
                    default:
                        parseUnknown();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalDataException(e);
        }
    }

    private long parseWayNode(Way w) throws XMLStreamException {
        if (parser.getAttributeValue(null, "ref") == null) {
            throwException(
                    tr("Missing mandatory attribute ''{0}'' on <nd> of way {1}.", "ref", Long.toString(w.getUniqueId()))
            );
        }
        long id = getLong("ref");
        if (id == 0) {
            throwException(
                    tr("Illegal value of attribute ''ref'' of element <nd>. Got {0}.", Long.toString(id))
            );
        }
        jumpToEnd();
        return id;
    }

    protected Relation parseRelation() throws XMLStreamException {
        try {
            return parseRelation(this::readCommon, this::parseRelationMembersAndTags);
        } catch (IllegalDataException e) {
            handleIllegalDataException(e);
        }
        return null;
    }

    private void parseRelationMembersAndTags(Relation r, Collection<RelationMemberData> members) throws IllegalDataException {
        try {
            while (parser.hasNext()) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (parser.getLocalName()) {
                    case "member":
                        members.add(parseRelationMember(r));
                        break;
                    case "tag":
                        parseTag(r);
                        break;
                    default:
                        parseUnknown();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalDataException(e);
        }
    }

    private RelationMemberData parseRelationMember(Relation r) throws XMLStreamException {
        RelationMemberData result = null;
        try {
            String ref = parser.getAttributeValue(null, "ref");
            String type = parser.getAttributeValue(null, "type");
            String role = parser.getAttributeValue(null, "role");
            result = parseRelationMember(r, ref, type, role);
            jumpToEnd();
        } catch (IllegalDataException e) {
            handleIllegalDataException(e);
        }
        return result;
    }

    private void parseChangeset(Long uploadChangesetId) throws XMLStreamException {

        Long id = null;
        if (parser.getAttributeValue(null, "id") != null) {
            id = getLong("id");
        }
        // Read changeset info if neither upload-changeset nor id are set, or if they are both set to the same value
        if (Objects.equals(id, uploadChangesetId)) {
            uploadChangeset = new Changeset(id != null ? id.intValue() : 0);
            while (true) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if ("tag".equals(parser.getLocalName())) {
                        parseTag(uploadChangeset);
                    } else {
                        parseUnknown();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT)
                    return;
            }
        } else {
            jumpToEnd(false);
        }
    }

    private void parseTag(Tagged t) throws XMLStreamException {
        String key = parser.getAttributeValue(null, "k");
        String value = parser.getAttributeValue(null, "v");
        try {
            parseTag(t, key, value);
        } catch (IllegalDataException e) {
            throwException(e);
        }
        jumpToEnd();
    }

    protected void parseUnknown(boolean printWarning) throws XMLStreamException {
        final String element = parser.getLocalName();
        if (printWarning && ("note".equals(element) || "meta".equals(element))) {
            // we know that Overpass API returns those elements
            Logging.debug(tr("Undefined element ''{0}'' found in input stream. Skipping.", element));
        } else if (printWarning) {
            Logging.info(tr("Undefined element ''{0}'' found in input stream. Skipping.", element));
        }
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                parseUnknown(false); /* no more warning for inner elements */
            } else if (event == XMLStreamConstants.END_ELEMENT)
                return;
        }
    }

    protected void parseUnknown() throws XMLStreamException {
        parseUnknown(true);
    }

    /**
     * When cursor is at the start of an element, moves it to the end tag of that element.
     * Nested content is skipped.
     *
     * This is basically the same code as parseUnknown(), except for the warnings, which
     * are displayed for inner elements and not at top level.
     * @param printWarning if {@code true}, a warning message will be printed if an unknown element is met
     * @throws XMLStreamException if there is an error processing the underlying XML source
     */
    protected final void jumpToEnd(boolean printWarning) throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                parseUnknown(printWarning);
            } else if (event == XMLStreamConstants.END_ELEMENT)
                return;
        }
    }

    protected final void jumpToEnd() throws XMLStreamException {
        jumpToEnd(true);
    }

    /**
     * Read out the common attributes and put them into current OsmPrimitive.
     * @param current primitive to update
     * @throws IllegalDataException if there is an error processing the underlying XML source
     */
    private void readCommon(PrimitiveData current) throws IllegalDataException {
        try {
            parseId(current, getLong("id"));
            parseTimestamp(current, parser.getAttributeValue(null, "timestamp"));
            parseUser(current, parser.getAttributeValue(null, "user"), parser.getAttributeValue(null, "uid"));
            parseVisible(current, parser.getAttributeValue(null, "visible"));
            parseVersion(current, parser.getAttributeValue(null, "version"));
            parseAction(current, parser.getAttributeValue(null, "action"));
            parseChangeset(current, parser.getAttributeValue(null, "changeset"));
        } catch (UncheckedParseException | XMLStreamException e) {
            throw new IllegalDataException(e);
        }
    }

    private long getLong(String name) throws XMLStreamException {
        String value = parser.getAttributeValue(null, name);
        try {
            return getLong(name, value);
        } catch (IllegalDataException e) {
            throwException(e);
        }
        return 0; // should not happen
    }

    /**
     * Exception thrown after user cancelation.
     */
    private static final class OsmParsingCanceledException extends XmlStreamParsingException implements ImportCancelException {
        /**
         * Constructs a new {@code OsmParsingCanceledException}.
         * @param msg The error message
         * @param location The parser location
         */
        OsmParsingCanceledException(String msg, Location location) {
            super(msg, location);
        }
    }

    @Override
    protected DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return doParseDataSet(source, progressMonitor, ir -> {
            try {
                setParser(XmlUtils.newSafeXMLInputFactory().createXMLStreamReader(ir));
                parse();
            } catch (XmlStreamParsingException | UncheckedParseException e) {
                throw new IllegalDataException(e.getMessage(), e);
            } catch (XMLStreamException e) {
                String msg = e.getMessage();
                Pattern p = Pattern.compile("Message: (.+)");
                Matcher m = p.matcher(msg);
                if (m.find()) {
                    msg = m.group(1);
                }
                if (e.getLocation() != null)
                    throw new IllegalDataException(tr("Line {0} column {1}: ",
                            e.getLocation().getLineNumber(), e.getLocation().getColumnNumber()) + msg, e);
                else
                    throw new IllegalDataException(msg, e);
            }
        });
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source the source input stream. Must not be null.
     * @param progressMonitor the progress monitor. If null, {@link NullProgressMonitor#INSTANCE} is assumed
     *
     * @return the dataset with the parsed data
     * @throws IllegalDataException if an error was found while parsing the data from the source
     * @throws IllegalArgumentException if source is null
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new OsmReader().doParseDataSet(source, progressMonitor);
    }
}
