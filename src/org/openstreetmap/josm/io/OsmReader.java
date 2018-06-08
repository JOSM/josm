// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;
import org.openstreetmap.josm.tools.date.DateUtils;

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
    private static volatile List<OsmServerReadPostprocessor> postprocessors;

    /** Register a new postprocessor.
     * @param pp postprocessor
     * @see #deregisterPostprocessor
     */
    public static void registerPostprocessor(OsmServerReadPostprocessor pp) {
        if (postprocessors == null) {
            postprocessors = new ArrayList<>();
        }
        postprocessors.add(pp);
    }

    /**
     * Deregister a postprocessor previously registered with {@link #registerPostprocessor}.
     * @param pp postprocessor
     * @see #registerPostprocessor
     */
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
        // Restricts visibility
    }

    protected void setParser(XMLStreamReader parser) {
        this.parser = parser;
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
        String v = parser.getAttributeValue(null, "version");
        if (v == null) {
            throwException(tr("Missing mandatory attribute ''{0}''.", "version"));
        }
        if (!"0.6".equals(v)) {
            throwException(tr("Unsupported version: {0}", v));
        }
        ds.setVersion(v);
        parsePolicy("download", policy -> ds.setDownloadPolicy(DownloadPolicy.of(policy)));
        parsePolicy("upload", policy -> ds.setUploadPolicy(UploadPolicy.of(policy)));
        if ("true".equalsIgnoreCase(parser.getAttributeValue(null, "locked"))) {
            ds.lock();
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
                default:
                    parseUnknown();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void parsePolicy(String key, Consumer<String> consumer) throws XMLStreamException {
        String policy = parser.getAttributeValue(null, key);
        if (policy != null) {
            try {
                consumer.accept(policy);
            } catch (IllegalArgumentException e) {
                throwException(MessageFormat.format("Illegal value for attribute ''{0}''. Got ''{1}''.", key, policy), e);
            }
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
                Logging.info("Bbox " + copy + " is out of the world, normalized to " + bounds);
            }
            DataSource src = new DataSource(bounds, origin);
            ds.addDataSource(src);
        } else {
            throwException(tr("Missing mandatory attributes on element ''bounds''. " +
                    "Got minlon=''{0}'',minlat=''{1}'',maxlon=''{2}'',maxlat=''{3}'', origin=''{4}''.",
                    minlon, minlat, maxlon, maxlat, origin
            ));
        }
        jumpToEnd();
    }

    protected Node parseNode() throws XMLStreamException {
        NodeData nd = new NodeData();
        String lat = parser.getAttributeValue(null, "lat");
        String lon = parser.getAttributeValue(null, "lon");
        LatLon ll = null;
        if (lat != null && lon != null) {
            try {
                ll = new LatLon(Double.parseDouble(lat), Double.parseDouble(lon));
                nd.setCoor(ll);
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        readCommon(nd);
        if (lat != null && lon != null && (ll == null || !ll.isValid())) {
            throwException(tr("Illegal value for attributes ''lat'', ''lon'' on node with ID {0}. Got ''{1}'', ''{2}''.",
                    Long.toString(nd.getId()), lat, lon));
        }
        Node n = new Node(nd.getId(), nd.getVersion());
        n.setVisible(nd.isVisible());
        n.load(nd);
        externalIdMap.put(nd.getPrimitiveId(), n);
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("tag".equals(parser.getLocalName())) {
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

        Collection<Long> nodeIds = new ArrayList<>();
        while (true) {
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
        if (w.isDeleted() && !nodeIds.isEmpty()) {
            Logging.info(tr("Deleted way {0} contains nodes", Long.toString(w.getUniqueId())));
            nodeIds = new ArrayList<>();
        }
        ways.put(wd.getUniqueId(), nodeIds);
        return w;
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
        RelationData rd = new RelationData();
        readCommon(rd);
        Relation r = new Relation(rd.getId(), rd.getVersion());
        r.setVisible(rd.isVisible());
        r.load(rd);
        externalIdMap.put(rd.getPrimitiveId(), r);

        Collection<RelationMemberData> members = new ArrayList<>();
        while (true) {
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
        if (r.isDeleted() && !members.isEmpty()) {
            Logging.info(tr("Deleted relation {0} contains members", Long.toString(r.getUniqueId())));
            members = new ArrayList<>();
        }
        relations.put(rd.getUniqueId(), members);
        return r;
    }

    private RelationMemberData parseRelationMember(Relation r) throws XMLStreamException {
        OsmPrimitiveType type = null;
        long id = 0;
        String value = parser.getAttributeValue(null, "ref");
        if (value == null) {
            throwException(tr("Missing attribute ''ref'' on member in relation {0}.", Long.toString(r.getUniqueId())));
        }
        try {
            id = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throwException(tr("Illegal value for attribute ''ref'' on member in relation {0}. Got {1}", Long.toString(r.getUniqueId()),
                    value), e);
        }
        value = parser.getAttributeValue(null, "type");
        if (value == null) {
            throwException(tr("Missing attribute ''type'' on member {0} in relation {1}.", Long.toString(id), Long.toString(r.getUniqueId())));
        }
        try {
            type = OsmPrimitiveType.fromApiTypeName(value);
        } catch (IllegalArgumentException e) {
            throwException(tr("Illegal value for attribute ''type'' on member {0} in relation {1}. Got {2}.",
                    Long.toString(id), Long.toString(r.getUniqueId()), value), e);
        }
        String role = parser.getAttributeValue(null, "role");

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
        if (key == null || value == null) {
            throwException(tr("Missing key or value attribute in tag."));
        } else if (Utils.isStripEmpty(key) && t instanceof AbstractPrimitive) {
            // #14199: Empty keys as ignored by AbstractPrimitive#put, but it causes problems to fix existing data
            // Drop the tag on import, but flag the primitive as modified
            ((AbstractPrimitive) t).setModified(true);
        } else {
            t.put(key.intern(), value.intern());
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
        } catch (NumberFormatException e) {
            throwException(MessageFormat.format("Illegal value for attribute ''uid''. Got ''{0}''.", uid), e);
        }
        return null;
    }

    /**
     * Read out the common attributes and put them into current OsmPrimitive.
     * @param current primitive to update
     * @throws XMLStreamException if there is an error processing the underlying XML source
     */
    private void readCommon(PrimitiveData current) throws XMLStreamException {
        current.setId(getLong("id"));
        if (current.getUniqueId() == 0) {
            throwException(tr("Illegal object with ID=0."));
        }

        String time = parser.getAttributeValue(null, "timestamp");
        if (time != null && !time.isEmpty()) {
            current.setRawTimestamp((int) (DateUtils.tsFromString(time)/1000));
        }

        String user = parser.getAttributeValue(null, "user");
        String uid = parser.getAttributeValue(null, "uid");
        current.setUser(createUser(uid, user));

        String visible = parser.getAttributeValue(null, "visible");
        if (visible != null) {
            current.setVisible(Boolean.parseBoolean(visible));
        }

        String versionString = parser.getAttributeValue(null, "version");
        int version = 0;
        if (versionString != null) {
            try {
                version = Integer.parseInt(versionString);
            } catch (NumberFormatException e) {
                throwException(tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.",
                        Long.toString(current.getUniqueId()), versionString), e);
            }
            switch (ds.getVersion()) {
            case "0.6":
                if (version <= 0 && !current.isNew()) {
                    throwException(tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.",
                            Long.toString(current.getUniqueId()), versionString));
                } else if (version < 0 && current.isNew()) {
                    Logging.warn(tr("Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.",
                            current.getUniqueId(), version, 0, "0.6"));
                    version = 0;
                }
                break;
            default:
                // should not happen. API version has been checked before
                throwException(tr("Unknown or unsupported API version. Got {0}.", ds.getVersion()));
            }
        } else {
            // version expected for OSM primitives with an id assigned by the server (id > 0), since API 0.6
            if (!current.isNew() && ds.getVersion() != null && "0.6".equals(ds.getVersion())) {
                throwException(tr("Missing attribute ''version'' on OSM primitive with ID {0}.", Long.toString(current.getUniqueId())));
            }
        }
        current.setVersion(version);

        String action = parser.getAttributeValue(null, "action");
        if (action == null) {
            // do nothing
        } else if ("delete".equals(action)) {
            current.setDeleted(true);
            current.setModified(current.isVisible());
        } else if ("modify".equals(action)) {
            current.setModified(true);
        }

        String v = parser.getAttributeValue(null, "changeset");
        if (v == null) {
            current.setChangesetId(0);
        } else {
            try {
                current.setChangesetId(Integer.parseInt(v));
            } catch (IllegalArgumentException e) {
                Logging.debug(e.getMessage());
                if (current.isNew()) {
                    // for a new primitive we just log a warning
                    Logging.info(tr("Illegal value for attribute ''changeset'' on new object {1}. Got {0}. Resetting to 0.",
                            v, current.getUniqueId()));
                    current.setChangesetId(0);
                } else {
                    // for an existing primitive this is a problem
                    throwException(tr("Illegal value for attribute ''changeset''. Got {0}.", v), e);
                }
            } catch (IllegalStateException e) {
                // thrown for positive changeset id on new primitives
                Logging.debug(e);
                Logging.info(e.getMessage());
                current.setChangesetId(0);
            }
            if (current.getChangesetId() <= 0) {
                if (current.isNew()) {
                    // for a new primitive we just log a warning
                    Logging.info(tr("Illegal value for attribute ''changeset'' on new object {1}. Got {0}. Resetting to 0.",
                            v, current.getUniqueId()));
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
            throwException(tr("Missing required attribute ''{0}''.", name));
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throwException(tr("Illegal long value for attribute ''{0}''. Got ''{1}''.", name, value), e);
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
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        ProgressMonitor.CancelListener cancelListener = () -> cancel = true;
        progressMonitor.addCancelListener(cancelListener);
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        try {
            progressMonitor.beginTask(tr("Prepare OSM data...", 2));
            progressMonitor.indeterminateSubTask(tr("Parsing OSM data..."));

            try (InputStreamReader ir = UTFInputStreamReader.create(source)) {
                setParser(XmlUtils.newSafeXMLInputFactory().createXMLStreamReader(ir));
                parse();
            }
            progressMonitor.worked(1);

            boolean readOnly = getDataSet().isLocked();

            progressMonitor.indeterminateSubTask(tr("Preparing data set..."));
            if (readOnly) {
                getDataSet().unlock();
            }
            prepareDataSet();
            if (readOnly) {
                getDataSet().lock();
            }
            progressMonitor.worked(1);

            // iterate over registered postprocessors and give them each a chance
            // to modify the dataset we have just loaded.
            if (postprocessors != null) {
                for (OsmServerReadPostprocessor pp : postprocessors) {
                    pp.postprocessDataSet(getDataSet(), progressMonitor);
                }
            }
            // Make sure postprocessors did not change the read-only state
            if (readOnly && !getDataSet().isLocked()) {
                getDataSet().lock();
            }
            return getDataSet();
        } catch (IllegalDataException e) {
            throw e;
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
        } catch (IOException e) {
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
     * @throws IllegalDataException if an error was found while parsing the data from the source
     * @throws IllegalArgumentException if source is null
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new OsmReader().doParseDataSet(source, progressMonitor);
    }
}
