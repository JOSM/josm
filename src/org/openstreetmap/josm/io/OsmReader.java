// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.DateUtils;

/**
 * Parser for the Osm Api. Read from an input stream and construct a dataset out of it.
 *
 * For each xml element, there is a dedicated method.
 * The XMLStreamReader cursor points to the start of the element, when the method is
 * entered, and it must point to the end of the same element, when it is exited.
 */
public class OsmReader extends AbstractReader {

    protected XMLStreamReader parser;

    protected boolean cancel;

    /** Used by plugins to register themselves as data postprocessors. */
    public static List<OsmServerReadPostprocessor> postprocessors;

    /** register a new postprocessor */
    public static void registerPostprocessor(OsmServerReadPostprocessor pp) {
        if (postprocessors == null) {
            postprocessors = new ArrayList<OsmServerReadPostprocessor>();
        }
        postprocessors.add(pp);
    }

    /** deregister a postprocessor previously registered with registerPostprocessor */
    public static void deregisterPostprocessor(OsmServerReadPostprocessor pp) {
        if (postprocessors != null) {
            postprocessors.remove(pp);
        }
    }

    /**
     * constructor (for private and subclasses use only)
     *
     * @see #parseDataSet(InputStream, ProgressMonitor)
     */
    protected OsmReader() {
    }

    protected void setParser(XMLStreamReader parser) {
        this.parser = parser;
    }

    protected void throwException(String msg, Throwable th) throws XMLStreamException {
        throw new OsmParsingException(msg, parser.getLocation(), th);
    }

    protected void throwException(String msg) throws XMLStreamException {
        throw new OsmParsingException(msg, parser.getLocation());
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
        if (parser.getLocalName().equals("osm")) {
            parseOsm();
        } else {
            parseUnknown();
        }
    }

    private void parseOsm() throws XMLStreamException {
        String v = parser.getAttributeValue(null, "version");
        if (v == null) {
            throwException(tr("Missing mandatory attribute ''{0}''.", "version"));
        }
        if (!(v.equals("0.5") || v.equals("0.6"))) {
            throwException(tr("Unsupported version: {0}", v));
        }
        ds.setVersion(v);
        String upload = parser.getAttributeValue(null, "upload");
        if (upload != null) {
            ds.setUploadDiscouraged(!Boolean.parseBoolean(upload));
        }
        String generator = parser.getAttributeValue(null, "generator");
        Long uploadChangesetId = null;
        if (parser.getAttributeValue(null, "upload-changeset") != null) {
            uploadChangesetId = getLong("upload-changeset");
        }
        while (true) {
            int event = parser.next();

            if (cancel) {
                cancel = false;
                throw new OsmParsingCanceledException(tr("Reading was canceled"), parser.getLocation());
            }

            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("bounds")) {
                    parseBounds(generator);
                } else if (parser.getLocalName().equals("node")) {
                    parseNode();
                } else if (parser.getLocalName().equals("way")) {
                    parseWay();
                } else if (parser.getLocalName().equals("relation")) {
                    parseRelation();
                } else if (parser.getLocalName().equals("changeset")) {
                    parseChangeset(uploadChangesetId);
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT)
                return;
        }
    }

    private void parseBounds(String generator) throws XMLStreamException {
        String minlon = parser.getAttributeValue(null, "minlon");
        String minlat = parser.getAttributeValue(null, "minlat");
        String maxlon = parser.getAttributeValue(null, "maxlon");
        String maxlat = parser.getAttributeValue(null, "maxlat");
        String origin = parser.getAttributeValue(null, "origin");
        if (minlon != null && maxlon != null && minlat != null && maxlat != null) {
            if (origin == null) {
                origin = generator;
            }
            Bounds bounds = new Bounds(
                    Double.parseDouble(minlat), Double.parseDouble(minlon),
                    Double.parseDouble(maxlat), Double.parseDouble(maxlon));
            if (bounds.isOutOfTheWorld()) {
                Bounds copy = new Bounds(bounds);
                bounds.normalize();
                Main.info("Bbox " + copy + " is out of the world, normalized to " + bounds);
            }
            DataSource src = new DataSource(bounds, origin);
            ds.dataSources.add(src);
        } else {
            throwException(tr(
                    "Missing mandatory attributes on element ''bounds''. Got minlon=''{0}'',minlat=''{1}'',maxlon=''{3}'',maxlat=''{4}'', origin=''{5}''.",
                    minlon, minlat, maxlon, maxlat, origin
            ));
        }
        jumpToEnd();
    }

    protected Node parseNode() throws XMLStreamException {
        NodeData nd = new NodeData();
        String lat = parser.getAttributeValue(null, "lat");
        String lon = parser.getAttributeValue(null, "lon");
        if (lat != null && lon != null) {
            nd.setCoor(new LatLon(Double.parseDouble(lat), Double.parseDouble(lon)));
        }
        readCommon(nd);
        Node n = new Node(nd.getId(), nd.getVersion());
        n.setVisible(nd.isVisible());
        n.load(nd);
        externalIdMap.put(nd.getPrimitiveId(), n);
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("tag")) {
                    parseTag(n);
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT)
                return n;
        }
    }

    protected Way parseWay() throws XMLStreamException {
        WayData wd = new WayData();
        readCommon(wd);
        Way w = new Way(wd.getId(), wd.getVersion());
        w.setVisible(wd.isVisible());
        w.load(wd);
        externalIdMap.put(wd.getPrimitiveId(), w);

        Collection<Long> nodeIds = new ArrayList<Long>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("nd")) {
                    nodeIds.add(parseWayNode(w));
                } else if (parser.getLocalName().equals("tag")) {
                    parseTag(w);
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        if (w.isDeleted() && !nodeIds.isEmpty()) {
            Main.info(tr("Deleted way {0} contains nodes", w.getUniqueId()));
            nodeIds = new ArrayList<Long>();
        }
        ways.put(wd.getUniqueId(), nodeIds);
        return w;
    }

    private long parseWayNode(Way w) throws XMLStreamException {
        if (parser.getAttributeValue(null, "ref") == null) {
            throwException(
                    tr("Missing mandatory attribute ''{0}'' on <nd> of way {1}.", "ref", w.getUniqueId())
            );
        }
        long id = getLong("ref");
        if (id == 0) {
            throwException(
                    tr("Illegal value of attribute ''ref'' of element <nd>. Got {0}.", id)
            );
        }
        jumpToEnd();
        return id;
    }

    protected Relation parseRelation() throws XMLStreamException {
        RelationData rd = new RelationData();
        readCommon(rd);
        Relation r = new Relation(rd.getId(), rd.getVersion());
        r.setVisible(rd.isVisible());
        r.load(rd);
        externalIdMap.put(rd.getPrimitiveId(), r);

        Collection<RelationMemberData> members = new ArrayList<RelationMemberData>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("member")) {
                    members.add(parseRelationMember(r));
                } else if (parser.getLocalName().equals("tag")) {
                    parseTag(r);
                } else {
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        if (r.isDeleted() && !members.isEmpty()) {
            Main.info(tr("Deleted relation {0} contains members", r.getUniqueId()));
            members = new ArrayList<RelationMemberData>();
        }
        relations.put(rd.getUniqueId(), members);
        return r;
    }

    private RelationMemberData parseRelationMember(Relation r) throws XMLStreamException {
        String role = null;
        OsmPrimitiveType type = null;
        long id = 0;
        String value = parser.getAttributeValue(null, "ref");
        if (value == null) {
            throwException(tr("Missing attribute ''ref'' on member in relation {0}.",r.getUniqueId()));
        }
        try {
            id = Long.parseLong(value);
        } catch(NumberFormatException e) {
            throwException(tr("Illegal value for attribute ''ref'' on member in relation {0}. Got {1}", Long.toString(r.getUniqueId()),value), e);
        }
        value = parser.getAttributeValue(null, "type");
        if (value == null) {
            throwException(tr("Missing attribute ''type'' on member {0} in relation {1}.", Long.toString(id), Long.toString(r.getUniqueId())));
        }
        try {
            type = OsmPrimitiveType.fromApiTypeName(value);
        } catch(IllegalArgumentException e) {
            throwException(tr("Illegal value for attribute ''type'' on member {0} in relation {1}. Got {2}.", Long.toString(id), Long.toString(r.getUniqueId()), value), e);
        }
        value = parser.getAttributeValue(null, "role");
        role = value;

        if (id == 0) {
            throwException(tr("Incomplete <member> specification with ref=0"));
        }
        jumpToEnd();
        return new RelationMemberData(role, type, id);
    }

    private void parseChangeset(Long uploadChangesetId) throws XMLStreamException {

        Long id = null;
        if (parser.getAttributeValue(null, "id") != null) {
            id = getLong("id");
        }
        // Read changeset info if neither upload-changeset nor id are set, or if they are both set to the same value
        if (id == uploadChangesetId || (id != null && id.equals(uploadChangesetId))) {
            uploadChangeset = new Changeset(id != null ? id.intValue() : 0);
            while (true) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (parser.getLocalName().equals("tag")) {
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
        if (key == null || value == null) {
            throwException(tr("Missing key or value attribute in tag."));
        }
        t.put(key.intern(), value.intern());
        jumpToEnd();
    }

    protected void parseUnknown(boolean printWarning) throws XMLStreamException {
        if (printWarning) {
            Main.info(tr("Undefined element ''{0}'' found in input stream. Skipping.", parser.getLocalName()));
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
     */
    private void jumpToEnd(boolean printWarning) throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                parseUnknown(printWarning);
            } else if (event == XMLStreamConstants.END_ELEMENT)
                return;
        }
    }

    private void jumpToEnd() throws XMLStreamException {
        jumpToEnd(true);
    }

    private User createUser(String uid, String name) throws XMLStreamException {
        if (uid == null) {
            if (name == null)
                return null;
            return User.createLocalUser(name);
        }
        try {
            long id = Long.parseLong(uid);
            return User.createOsmUser(id, name);
        } catch(NumberFormatException e) {
            throwException(MessageFormat.format("Illegal value for attribute ''uid''. Got ''{0}''.", uid), e);
        }
        return null;
    }

    /**
     * Read out the common attributes and put them into current OsmPrimitive.
     */
    private void readCommon(PrimitiveData current) throws XMLStreamException {
        current.setId(getLong("id"));
        if (current.getUniqueId() == 0) {
            throwException(tr("Illegal object with ID=0."));
        }

        String time = parser.getAttributeValue(null, "timestamp");
        if (time != null && time.length() != 0) {
            current.setTimestamp(DateUtils.fromString(time));
        }

        // user attribute added in 0.4 API
        String user = parser.getAttributeValue(null, "user");
        // uid attribute added in 0.6 API
        String uid = parser.getAttributeValue(null, "uid");
        current.setUser(createUser(uid, user));

        // visible attribute added in 0.4 API
        String visible = parser.getAttributeValue(null, "visible");
        if (visible != null) {
            current.setVisible(Boolean.parseBoolean(visible));
        }

        String versionString = parser.getAttributeValue(null, "version");
        int version = 0;
        if (versionString != null) {
            try {
                version = Integer.parseInt(versionString);
            } catch(NumberFormatException e) {
                throwException(tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.", Long.toString(current.getUniqueId()), versionString), e);
            }
            if (ds.getVersion().equals("0.6")){
                if (version <= 0 && current.getUniqueId() > 0) {
                    throwException(tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.", Long.toString(current.getUniqueId()), versionString));
                } else if (version < 0 && current.getUniqueId() <= 0) {
                    Main.warn(tr("Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.", current.getUniqueId(), version, 0, "0.6"));
                    version = 0;
                }
            } else if (ds.getVersion().equals("0.5")) {
                if (version <= 0 && current.getUniqueId() > 0) {
                    Main.warn(tr("Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.", current.getUniqueId(), version, 1, "0.5"));
                    version = 1;
                } else if (version < 0 && current.getUniqueId() <= 0) {
                    Main.warn(tr("Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.", current.getUniqueId(), version, 0, "0.5"));
                    version = 0;
                }
            } else {
                // should not happen. API version has been checked before
                throwException(tr("Unknown or unsupported API version. Got {0}.", ds.getVersion()));
            }
        } else {
            // version expected for OSM primitives with an id assigned by the server (id > 0), since API 0.6
            //
            if (current.getUniqueId() > 0 && ds.getVersion() != null && ds.getVersion().equals("0.6")) {
                throwException(tr("Missing attribute ''version'' on OSM primitive with ID {0}.", Long.toString(current.getUniqueId())));
            } else if (current.getUniqueId() > 0 && ds.getVersion() != null && ds.getVersion().equals("0.5")) {
                // default version in 0.5 files for existing primitives
                Main.warn(tr("Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.", current.getUniqueId(), version, 1, "0.5"));
                version= 1;
            } else if (current.getUniqueId() <= 0 && ds.getVersion() != null && ds.getVersion().equals("0.5")) {
                // default version in 0.5 files for new primitives, no warning necessary. This is
                // (was) legal in API 0.5
                version= 0;
            }
        }
        current.setVersion(version);

        String action = parser.getAttributeValue(null, "action");
        if (action == null) {
            // do nothing
        } else if (action.equals("delete")) {
            current.setDeleted(true);
            current.setModified(current.isVisible());
        } else if (action.equals("modify")) {
            current.setModified(true);
        }

        String v = parser.getAttributeValue(null, "changeset");
        if (v == null) {
            current.setChangesetId(0);
        } else {
            try {
                current.setChangesetId(Integer.parseInt(v));
            } catch(NumberFormatException e) {
                if (current.getUniqueId() <= 0) {
                    // for a new primitive we just log a warning
                    Main.info(tr("Illegal value for attribute ''changeset'' on new object {1}. Got {0}. Resetting to 0.", v, current.getUniqueId()));
                    current.setChangesetId(0);
                } else {
                    // for an existing primitive this is a problem
                    throwException(tr("Illegal value for attribute ''changeset''. Got {0}.", v), e);
                }
            }
            if (current.getChangesetId() <=0) {
                if (current.getUniqueId() <= 0) {
                    // for a new primitive we just log a warning
                    Main.info(tr("Illegal value for attribute ''changeset'' on new object {1}. Got {0}. Resetting to 0.", v, current.getUniqueId()));
                    current.setChangesetId(0);
                } else {
                    // for an existing primitive this is a problem
                    throwException(tr("Illegal value for attribute ''changeset''. Got {0}.", v));
                }
            }
        }
    }

    private long getLong(String name) throws XMLStreamException {
        String value = parser.getAttributeValue(null, name);
        if (value == null) {
            throwException(tr("Missing required attribute ''{0}''.",name));
        }
        try {
            return Long.parseLong(value);
        } catch(NumberFormatException e) {
            throwException(tr("Illegal long value for attribute ''{0}''. Got ''{1}''.",name, value), e);
        }
        return 0; // should not happen
    }

    private static class OsmParsingException extends XMLStreamException {

        public OsmParsingException(String msg, Location location) {
            super(msg); /* cannot use super(msg, location) because it messes with the message preventing localization */
            this.location = location;
        }

        public OsmParsingException(String msg, Location location, Throwable th) {
            super(msg, th);
            this.location = location;
        }

        @Override
        public String getMessage() {
            String msg = super.getMessage();
            if (msg == null) {
                msg = getClass().getName();
            }
            if (getLocation() == null)
                return msg;
            msg = msg + " " + tr("(at line {0}, column {1})", getLocation().getLineNumber(), getLocation().getColumnNumber());
            return msg;
        }
    }

    /**
     * Exception thrown after user cancelation.
     */
    private static final class OsmParsingCanceledException extends OsmParsingException implements ImportCancelException {
        /**
         * Constructs a new {@code OsmParsingCanceledException}.
         * @param msg The error message
         * @param location The parser location
         */
        public OsmParsingCanceledException(String msg, Location location) {
            super(msg, location);
        }
    }

    protected DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        ProgressMonitor.CancelListener cancelListener = new ProgressMonitor.CancelListener() {
            @Override public void operationCanceled() {
                cancel = true;
            }
        };
        progressMonitor.addCancelListener(cancelListener);
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        try {
            progressMonitor.beginTask(tr("Prepare OSM data...", 2));
            progressMonitor.indeterminateSubTask(tr("Parsing OSM data..."));

            InputStreamReader ir = UTFInputStreamReader.create(source);
            XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(ir);
            setParser(parser);
            parse();
            progressMonitor.worked(1);

            progressMonitor.indeterminateSubTask(tr("Preparing data set..."));
            prepareDataSet();
            progressMonitor.worked(1);

            // iterate over registered postprocessors and give them each a chance
            // to modify the dataset we have just loaded.
            if (postprocessors != null) {
                for (OsmServerReadPostprocessor pp : postprocessors) {
                    pp.postprocessDataSet(getDataSet(), progressMonitor);
                }
            }
            return getDataSet();
        } catch(IllegalDataException e) {
            throw e;
        } catch(OsmParsingException e) {
            throw new IllegalDataException(e.getMessage(), e);
        } catch(XMLStreamException e) {
            String msg = e.getMessage();
            Pattern p = Pattern.compile("Message: (.+)");
            Matcher m = p.matcher(msg);
            if (m.find()) {
                msg = m.group(1);
            }
            if (e.getLocation() != null)
                throw new IllegalDataException(tr("Line {0} column {1}: ", e.getLocation().getLineNumber(), e.getLocation().getColumnNumber()) + msg, e);
            else
                throw new IllegalDataException(msg, e);
        } catch(Exception e) {
            throw new IllegalDataException(e);
        } finally {
            progressMonitor.finishTask();
            progressMonitor.removeCancelListener(cancelListener);
        }
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source the source input stream. Must not be null.
     * @param progressMonitor  the progress monitor. If null, {@link NullProgressMonitor#INSTANCE} is assumed
     *
     * @return the dataset with the parsed data
     * @throws IllegalDataException thrown if the an error was found while parsing the data from the source
     * @throws IllegalArgumentException thrown if source is null
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new OsmReader().doParseDataSet(source, progressMonitor);
    }
}
