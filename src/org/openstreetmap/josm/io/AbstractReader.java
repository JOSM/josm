// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalLong;
import java.util.function.Consumer;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Abstract Reader, allowing other implementations than OsmReader (PbfReader in PBF plugin for example)
 * @author Vincent
 * @since 4490
 */
public abstract class AbstractReader {

    /** Used by plugins to register themselves as data postprocessors. */
    private static volatile List<OsmServerReadPostprocessor> postprocessors;

    protected boolean cancel;

    /**
     * Register a new postprocessor.
     * @param pp postprocessor
     * @see #deregisterPostprocessor
     * @since 14119 (moved from OsmReader)
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
     * @since 14119 (moved from OsmReader)
     */
    public static void deregisterPostprocessor(OsmServerReadPostprocessor pp) {
        if (postprocessors != null) {
            postprocessors.remove(pp);
        }
    }

    /**
     * The dataset to add parsed objects to.
     */
    protected DataSet ds = new DataSet();

    protected Changeset uploadChangeset;

    /** the map from external ids to read OsmPrimitives. External ids are
     * longs too, but in contrast to internal ids negative values are used
     * to identify primitives unknown to the OSM server
     */
    protected final Map<PrimitiveId, OsmPrimitive> externalIdMap = new HashMap<>();

    /**
     * Data structure for the remaining way objects
     */
    protected final Map<Long, Collection<Long>> ways = new HashMap<>();

    /**
     * Data structure for relation objects
     */
    protected final Map<Long, Collection<RelationMemberData>> relations = new HashMap<>();

    /**
     * Replies the parsed data set
     *
     * @return the parsed data set
     */
    public DataSet getDataSet() {
        return ds;
    }

    /**
     * Iterate over registered postprocessors and give them each a chance to modify the dataset we have just loaded.
     * @param progressMonitor Progress monitor
     */
    protected void callPostProcessors(ProgressMonitor progressMonitor) {
        if (postprocessors != null) {
            for (OsmServerReadPostprocessor pp : postprocessors) {
                pp.postprocessDataSet(getDataSet(), progressMonitor);
            }
        }
    }

    /**
     * Processes the parsed nodes after parsing. Just adds them to
     * the dataset
     *
     */
    protected void processNodesAfterParsing() {
        for (OsmPrimitive primitive: externalIdMap.values()) {
            if (primitive instanceof Node) {
                this.ds.addPrimitive(primitive);
            }
        }
    }

    /**
     * Processes the ways after parsing. Rebuilds the list of nodes of each way and
     * adds the way to the dataset
     *
     * @throws IllegalDataException if a data integrity problem is detected
     */
    protected void processWaysAfterParsing() throws IllegalDataException {
        for (Entry<Long, Collection<Long>> entry : ways.entrySet()) {
            Long externalWayId = entry.getKey();
            Way w = (Way) externalIdMap.get(new SimplePrimitiveId(externalWayId, OsmPrimitiveType.WAY));
            List<Node> wayNodes = new ArrayList<>();
            for (long id : entry.getValue()) {
                Node n = (Node) externalIdMap.get(new SimplePrimitiveId(id, OsmPrimitiveType.NODE));
                if (n == null) {
                    if (id <= 0)
                        throw new IllegalDataException(
                                tr("Way with external ID ''{0}'' includes missing node with external ID ''{1}''.",
                                        Long.toString(externalWayId),
                                        Long.toString(id)));
                    // create an incomplete node if necessary
                    n = (Node) ds.getPrimitiveById(id, OsmPrimitiveType.NODE);
                    if (n == null) {
                        n = new Node(id);
                        ds.addPrimitive(n);
                    }
                }
                if (n.isDeleted()) {
                    Logging.info(tr("Deleted node {0} is part of way {1}", Long.toString(id), Long.toString(w.getId())));
                } else {
                    wayNodes.add(n);
                }
            }
            w.setNodes(wayNodes);
            if (w.hasIncompleteNodes()) {
                Logging.info(tr("Way {0} with {1} nodes is incomplete because at least one node was missing in the loaded data.",
                        Long.toString(externalWayId), w.getNodesCount()));
            }
            ds.addPrimitive(w);
        }
    }

    /**
     * Completes the parsed relations with its members.
     *
     * @throws IllegalDataException if a data integrity problem is detected, i.e. if a
     * relation member refers to a local primitive which wasn't available in the data
     */
    protected void processRelationsAfterParsing() throws IllegalDataException {

        // First add all relations to make sure that when relation reference other relation, the referenced will be already in dataset
        for (Long externalRelationId : relations.keySet()) {
            Relation relation = (Relation) externalIdMap.get(
                    new SimplePrimitiveId(externalRelationId, OsmPrimitiveType.RELATION)
            );
            ds.addPrimitive(relation);
        }

        for (Entry<Long, Collection<RelationMemberData>> entry : relations.entrySet()) {
            Long externalRelationId = entry.getKey();
            Relation relation = (Relation) externalIdMap.get(
                    new SimplePrimitiveId(externalRelationId, OsmPrimitiveType.RELATION)
            );
            List<RelationMember> relationMembers = new ArrayList<>();
            for (RelationMemberData rm : entry.getValue()) {
                // lookup the member from the map of already created primitives
                OsmPrimitive primitive = externalIdMap.get(new SimplePrimitiveId(rm.getMemberId(), rm.getMemberType()));

                if (primitive == null) {
                    if (rm.getMemberId() <= 0)
                        // relation member refers to a primitive with a negative id which was not
                        // found in the data. This is always a data integrity problem and we abort
                        // with an exception
                        //
                        throw new IllegalDataException(
                                tr("Relation with external id ''{0}'' refers to a missing primitive with external id ''{1}''.",
                                        Long.toString(externalRelationId),
                                        Long.toString(rm.getMemberId())));

                    // member refers to OSM primitive which was not present in the parsed data
                    // -> create a new incomplete primitive and add it to the dataset
                    //
                    primitive = ds.getPrimitiveById(rm.getMemberId(), rm.getMemberType());
                    if (primitive == null) {
                        switch (rm.getMemberType()) {
                        case NODE:
                            primitive = new Node(rm.getMemberId()); break;
                        case WAY:
                            primitive = new Way(rm.getMemberId()); break;
                        case RELATION:
                            primitive = new Relation(rm.getMemberId()); break;
                        default: throw new AssertionError(); // can't happen
                        }

                        ds.addPrimitive(primitive);
                        externalIdMap.put(new SimplePrimitiveId(rm.getMemberId(), rm.getMemberType()), primitive);
                    }
                }
                if (primitive.isDeleted()) {
                    Logging.info(tr("Deleted member {0} is used by relation {1}",
                            Long.toString(primitive.getId()), Long.toString(relation.getId())));
                } else {
                    relationMembers.add(new RelationMember(rm.getRole(), primitive));
                }
            }
            relation.setMembers(relationMembers);
        }
    }

    protected void processChangesetAfterParsing() {
        if (uploadChangeset != null) {
            for (Map.Entry<String, String> e : uploadChangeset.getKeys().entrySet()) {
                ds.addChangeSetTag(e.getKey(), e.getValue());
            }
        }
    }

    protected final void prepareDataSet() throws IllegalDataException {
        ds.beginUpdate();
        try {
            processNodesAfterParsing();
            processWaysAfterParsing();
            processRelationsAfterParsing();
            processChangesetAfterParsing();
        } finally {
            ds.endUpdate();
        }
    }

    protected abstract DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException;

    @FunctionalInterface
    protected interface ParserWorker {
        /**
         * Effectively parses the file, depending on the format (XML, JSON, etc.)
         * @param ir input stream reader
         * @throws IllegalDataException in case of invalid data
         * @throws IOException in case of I/O error
         */
        void accept(InputStreamReader ir) throws IllegalDataException, IOException;
    }

    protected final DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor, ParserWorker parserWorker)
            throws IllegalDataException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        ProgressMonitor.CancelListener cancelListener = () -> cancel = true;
        progressMonitor.addCancelListener(cancelListener);
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        try {
            progressMonitor.beginTask(tr("Prepare OSM data..."), 4); // read, prepare, post-process, render
            progressMonitor.indeterminateSubTask(tr("Parsing OSM data..."));

            try (InputStreamReader ir = UTFInputStreamReader.create(source)) {
                parserWorker.accept(ir);
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
            progressMonitor.indeterminateSubTask(tr("Post-processing data set..."));
            // iterate over registered postprocessors and give them each a chance
            // to modify the dataset we have just loaded.
            callPostProcessors(progressMonitor);
            progressMonitor.worked(1);
            progressMonitor.indeterminateSubTask(tr("Rendering data set..."));
            // Make sure postprocessors did not change the read-only state
            if (readOnly && !getDataSet().isLocked()) {
                getDataSet().lock();
            }
            return getDataSet();
        } catch (IllegalDataException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalDataException(e);
        } finally {
            OptionalLong minId = externalIdMap.values().stream().mapToLong(AbstractPrimitive::getUniqueId).min();
            if (minId.isPresent() && minId.getAsLong() < AbstractPrimitive.currentUniqueId()) {
                AbstractPrimitive.advanceUniqueId(minId.getAsLong());
            }
            progressMonitor.finishTask();
            progressMonitor.removeCancelListener(cancelListener);
        }
    }

    protected final long getLong(String name, String value) throws IllegalDataException {
        if (value == null) {
            throw new IllegalDataException(tr("Missing required attribute ''{0}''.", name));
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalDataException(tr("Illegal long value for attribute ''{0}''. Got ''{1}''.", name, value), e);
        }
    }

    protected final void parseVersion(String version) throws IllegalDataException {
        validateVersion(version);
        ds.setVersion(version);
    }

    private static void validateVersion(String version) throws IllegalDataException {
        if (version == null) {
            throw new IllegalDataException(tr("Missing mandatory attribute ''{0}''.", "version"));
        }
        if (!"0.6".equals(version)) {
            throw new IllegalDataException(tr("Unsupported version: {0}", version));
        }
    }

    protected final void parseDownloadPolicy(String key, String downloadPolicy) throws IllegalDataException {
        parsePolicy(key, downloadPolicy, policy -> ds.setDownloadPolicy(DownloadPolicy.of(policy)));
    }

    protected final void parseUploadPolicy(String key, String uploadPolicy) throws IllegalDataException {
        parsePolicy(key, uploadPolicy, policy -> ds.setUploadPolicy(UploadPolicy.of(policy)));
    }

    private static void parsePolicy(String key, String policy, Consumer<String> consumer) throws IllegalDataException {
        if (policy != null) {
            try {
                consumer.accept(policy);
            } catch (IllegalArgumentException e) {
                throw new IllegalDataException(MessageFormat.format(
                        "Illegal value for attribute ''{0}''. Got ''{1}''.", key, policy), e);
            }
        }
    }

    protected final void parseLocked(String locked) {
        if ("true".equalsIgnoreCase(locked)) {
            ds.lock();
        }
    }

    protected final void parseBounds(String generator, String minlon, String minlat, String maxlon, String maxlat, String origin)
            throws IllegalDataException {
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
            ds.addDataSource(new DataSource(bounds, origin));
        } else {
            throw new IllegalDataException(tr("Missing mandatory attributes on element ''bounds''. " +
                    "Got minlon=''{0}'',minlat=''{1}'',maxlon=''{2}'',maxlat=''{3}'', origin=''{4}''.",
                    minlon, minlat, maxlon, maxlat, origin
            ));
        }
    }

    protected final void parseId(PrimitiveData current, long id) throws IllegalDataException {
        current.setId(id);
        if (current.getUniqueId() == 0) {
            throw new IllegalDataException(tr("Illegal object with ID=0."));
        }
    }

    protected final void parseTimestamp(PrimitiveData current, String time) {
        if (time != null && !time.isEmpty()) {
            current.setRawTimestamp((int) (DateUtils.tsFromString(time)/1000));
        }
    }

    private static User createUser(String uid, String name) throws IllegalDataException {
        if (uid == null) {
            if (name == null)
                return null;
            return User.createLocalUser(name);
        }
        try {
            return User.createOsmUser(Long.parseLong(uid), name);
        } catch (NumberFormatException e) {
            throw new IllegalDataException(MessageFormat.format("Illegal value for attribute ''uid''. Got ''{0}''.", uid), e);
        }
    }

    protected final void parseUser(PrimitiveData current, String user, long uid) {
        current.setUser(User.createOsmUser(uid, user));
    }

    protected final void parseUser(PrimitiveData current, String user, String uid) throws IllegalDataException {
        current.setUser(createUser(uid, user));
    }

    protected final void parseVisible(PrimitiveData current, String visible) {
        if (visible != null) {
            current.setVisible(Boolean.parseBoolean(visible));
        }
    }

    protected final void parseVersion(PrimitiveData current, String versionString) throws IllegalDataException {
        int version = 0;
        if (versionString != null) {
            try {
                version = Integer.parseInt(versionString);
            } catch (NumberFormatException e) {
                throw new IllegalDataException(
                        tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.",
                        Long.toString(current.getUniqueId()), versionString), e);
            }
            parseVersion(current, version);
        } else {
            // version expected for OSM primitives with an id assigned by the server (id > 0), since API 0.6
            if (!current.isNew() && ds.getVersion() != null && "0.6".equals(ds.getVersion())) {
                throw new IllegalDataException(
                        tr("Missing attribute ''version'' on OSM primitive with ID {0}.", Long.toString(current.getUniqueId())));
            }
        }
    }

    protected final void parseVersion(PrimitiveData current, int version) throws IllegalDataException {
        switch (ds.getVersion()) {
        case "0.6":
            if (version <= 0 && !current.isNew()) {
                throw new IllegalDataException(
                        tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.",
                        Long.toString(current.getUniqueId()), version));
            } else if (version < 0 && current.isNew()) {
                Logging.warn(tr("Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.",
                        current.getUniqueId(), version, 0, "0.6"));
                version = 0;
            }
            break;
        default:
            // should not happen. API version has been checked before
            throw new IllegalDataException(tr("Unknown or unsupported API version. Got {0}.", ds.getVersion()));
        }
        current.setVersion(version);
    }

    protected final void parseAction(PrimitiveData current, String action) {
        if (action == null) {
            // do nothing
        } else if ("delete".equals(action)) {
            current.setDeleted(true);
            current.setModified(current.isVisible());
        } else if ("modify".equals(action)) {
            current.setModified(true);
        }
    }

    private static void handleIllegalChangeset(PrimitiveData current, IllegalArgumentException e, Object v)
            throws IllegalDataException {
        Logging.debug(e.getMessage());
        if (current.isNew()) {
            // for a new primitive we just log a warning
            Logging.info(tr("Illegal value for attribute ''changeset'' on new object {1}. Got {0}. Resetting to 0.",
                    v, current.getUniqueId()));
            current.setChangesetId(0);
        } else {
            // for an existing primitive this is a problem
            throw new IllegalDataException(tr("Illegal value for attribute ''changeset''. Got {0}.", v), e);
        }
    }

    protected final void parseChangeset(PrimitiveData current, String v) throws IllegalDataException {
        if (v == null) {
            current.setChangesetId(0);
        } else {
            try {
                parseChangeset(current, Integer.parseInt(v));
            } catch (NumberFormatException e) {
                handleIllegalChangeset(current, e, v);
            }
        }
    }

    protected final void parseChangeset(PrimitiveData current, int v) throws IllegalDataException {
        try {
            current.setChangesetId(v);
        } catch (IllegalArgumentException e) {
            handleIllegalChangeset(current, e, v);
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
            } else if (current.getChangesetId() < 0) {
                // for an existing primitive this is a problem only for negative ids (GDPR extracts are set to 0)
                throw new IllegalDataException(tr("Illegal value for attribute ''changeset''. Got {0}.", v));
            }
        }
    }

    protected final void parseTag(Tagged t, String key, String value) throws IllegalDataException {
        if (key == null || value == null) {
            throw new IllegalDataException(tr("Missing key or value attribute in tag."));
        } else if (Utils.isStripEmpty(key) && t instanceof AbstractPrimitive) {
            // #14199: Empty keys as ignored by AbstractPrimitive#put, but it causes problems to fix existing data
            // Drop the tag on import, but flag the primitive as modified
            ((AbstractPrimitive) t).setModified(true);
        } else {
            t.put(key.intern(), value.intern());
        }
    }

    @FunctionalInterface
    protected interface CommonReader {
        /**
         * Reads the common primitive attributes and sets them in {@code pd}
         * @param pd primitive data to update
         * @throws IllegalDataException in case of invalid data
         */
        void accept(PrimitiveData pd) throws IllegalDataException;
    }

    @FunctionalInterface
    protected interface NodeReader {
        /**
         * Reads the node tags.
         * @param n node
         * @throws IllegalDataException in case of invalid data
         */
        void accept(Node n) throws IllegalDataException;
    }

    @FunctionalInterface
    protected interface WayReader {
        /**
         * Reads the way nodes and tags.
         * @param w way
         * @param nodeIds collection of resulting node ids
         * @throws IllegalDataException in case of invalid data
         */
        void accept(Way w, Collection<Long> nodeIds) throws IllegalDataException;
    }

    @FunctionalInterface
    protected interface RelationReader {
        /**
         * Reads the relation members and tags.
         * @param r relation
         * @param members collection of resulting members
         * @throws IllegalDataException in case of invalid data
         */
        void accept(Relation r, Collection<RelationMemberData> members) throws IllegalDataException;
    }

    private static boolean areLatLonDefined(String lat, String lon) {
        return lat != null && lon != null;
    }

    private static boolean areLatLonDefined(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    protected OsmPrimitive buildPrimitive(PrimitiveData pd) {
        OsmPrimitive p;
        if (pd.getUniqueId() < AbstractPrimitive.currentUniqueId()) {
            p = pd.getType().newInstance(pd.getUniqueId(), true);
        } else {
            p = pd.getType().newVersionedInstance(pd.getId(), pd.getVersion());
        }
        p.setVisible(pd.isVisible());
        p.load(pd);
        externalIdMap.put(pd.getPrimitiveId(), p);
        return p;
    }

    private Node addNode(NodeData nd, NodeReader nodeReader) throws IllegalDataException {
        Node n = (Node) buildPrimitive(nd);
        nodeReader.accept(n);
        return n;
    }

    protected final Node parseNode(double lat, double lon, CommonReader commonReader, NodeReader nodeReader)
            throws IllegalDataException {
        NodeData nd = new NodeData(0);
        LatLon ll = null;
        if (areLatLonDefined(lat, lon)) {
            try {
                ll = new LatLon(lat, lon);
                nd.setCoor(ll);
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        commonReader.accept(nd);
        if (areLatLonDefined(lat, lon) && (ll == null || !ll.isValid())) {
            throw new IllegalDataException(tr("Illegal value for attributes ''lat'', ''lon'' on node with ID {0}. Got ''{1}'', ''{2}''.",
                    Long.toString(nd.getId()), lat, lon));
        }
        return addNode(nd, nodeReader);
    }

    protected final Node parseNode(String lat, String lon, CommonReader commonReader, NodeReader nodeReader)
            throws IllegalDataException {
        NodeData nd = new NodeData();
        LatLon ll = null;
        if (areLatLonDefined(lat, lon)) {
            try {
                ll = new LatLon(Double.parseDouble(lat), Double.parseDouble(lon));
                nd.setCoor(ll);
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        commonReader.accept(nd);
        if (areLatLonDefined(lat, lon) && (ll == null || !ll.isValid())) {
            throw new IllegalDataException(tr("Illegal value for attributes ''lat'', ''lon'' on node with ID {0}. Got ''{1}'', ''{2}''.",
                    Long.toString(nd.getId()), lat, lon));
        }
        return addNode(nd, nodeReader);
    }

    protected final Way parseWay(CommonReader commonReader, WayReader wayReader) throws IllegalDataException {
        WayData wd = new WayData(0);
        commonReader.accept(wd);
        Way w = (Way) buildPrimitive(wd);

        Collection<Long> nodeIds = new ArrayList<>();
        wayReader.accept(w, nodeIds);
        if (w.isDeleted() && !nodeIds.isEmpty()) {
            Logging.info(tr("Deleted way {0} contains nodes", Long.toString(w.getUniqueId())));
            nodeIds = new ArrayList<>();
        }
        ways.put(wd.getUniqueId(), nodeIds);
        return w;
    }

    protected final Relation parseRelation(CommonReader commonReader, RelationReader relationReader) throws IllegalDataException {
        RelationData rd = new RelationData(0);
        commonReader.accept(rd);
        Relation r = (Relation) buildPrimitive(rd);

        Collection<RelationMemberData> members = new ArrayList<>();
        relationReader.accept(r, members);
        if (r.isDeleted() && !members.isEmpty()) {
            Logging.info(tr("Deleted relation {0} contains members", Long.toString(r.getUniqueId())));
            members = new ArrayList<>();
        }
        relations.put(rd.getUniqueId(), members);
        return r;
    }

    protected final RelationMemberData parseRelationMember(Relation r, String ref, String type, String role) throws IllegalDataException {
        if (ref == null) {
            throw new IllegalDataException(tr("Missing attribute ''ref'' on member in relation {0}.",
                    Long.toString(r.getUniqueId())));
        }
        try {
            return parseRelationMember(r, Long.parseLong(ref), type, role);
        } catch (NumberFormatException e) {
            throw new IllegalDataException(tr("Illegal value for attribute ''ref'' on member in relation {0}. Got {1}",
                    Long.toString(r.getUniqueId()), ref), e);
        }
    }

    protected final RelationMemberData parseRelationMember(Relation r, long id, String type, String role) throws IllegalDataException {
        if (id == 0) {
            throw new IllegalDataException(tr("Incomplete <member> specification with ref=0"));
        }
        if (type == null) {
            throw new IllegalDataException(tr("Missing attribute ''type'' on member {0} in relation {1}.",
                    Long.toString(id), Long.toString(r.getUniqueId())));
        }
        try {
            return new RelationMemberData(role, OsmPrimitiveType.fromApiTypeName(type), id);
        } catch (IllegalArgumentException e) {
            throw new IllegalDataException(tr("Illegal value for attribute ''type'' on member {0} in relation {1}. Got {2}.",
                    Long.toString(id), Long.toString(r.getUniqueId()), type), e);
        }
    }
}
