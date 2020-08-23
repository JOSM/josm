// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * OsmServerBackreferenceReader fetches the primitives from the OSM server which
 * refer to a specific primitive. For a {@link org.openstreetmap.josm.data.osm.Node Node}, ways and relations are retrieved
 * which refer to the node. For a {@link Way} or a {@link Relation}, only relations are read.
 *
 * OsmServerBackreferenceReader uses the API calls <code>[node|way|relation]/#id/relations</code>
 * and  <code>node/#id/ways</code> to retrieve the referring primitives. The default behaviour
 * of these calls is to reply incomplete primitives only.
 *
 * If you set {@link #setReadFull(boolean)} to true this reader uses a {@link MultiFetchServerObjectReader}
 * to complete incomplete primitives.
 *
 * @since 1806
 */
public class OsmServerBackreferenceReader extends OsmServerReader {

    /** the id of the primitive whose referrers are to be read */
    private final long id;
    /** the type of the primitive */
    private final OsmPrimitiveType primitiveType;
    /** true if this reader should complete incomplete primitives */
    private boolean readFull;
    /** true if this reader should allow incomplete parent ways */
    private boolean allowIncompleteParentWays;

    /**
     * constructor
     *
     * @param primitive  the primitive to be read. Must not be null. primitive.id &gt; 0 expected
     *
     * @throws IllegalArgumentException if primitive is null
     * @throws IllegalArgumentException if primitive.id &lt;= 0
     */
    public OsmServerBackreferenceReader(OsmPrimitive primitive) {
        CheckParameterUtil.ensureThat(primitive.getUniqueId() > 0, "id > 0");
        this.id = primitive.getId();
        this.primitiveType = OsmPrimitiveType.from(primitive);
        this.readFull = false;
    }

    /**
     * constructor
     *
     * @param id  the id of the primitive. &gt; 0 expected
     * @param type the type of the primitive. Must not be null.
     *
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if type is null
     */
    public OsmServerBackreferenceReader(long id, OsmPrimitiveType type) {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got ''{1}''.", "id", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        this.id = id;
        this.primitiveType = type;
        this.readFull = false;
    }

    /**
     * Creates a back reference reader for given primitive
     *
     * @param primitive the primitive
     * @param readFull <code>true</code>, if referrers should be read fully (i.e. including their immediate children)
     *
     */
    public OsmServerBackreferenceReader(OsmPrimitive primitive, boolean readFull) {
        this(primitive);
        this.readFull = readFull;
    }

    /**
     * Creates a back reference reader for given primitive id
     *
     * @param id the id of the primitive whose referrers are to be read
     * @param type the type of the primitive
     * @param readFull true, if referrers should be read fully (i.e. including their immediate children)
     *
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if type is null
     */
    public OsmServerBackreferenceReader(long id, OsmPrimitiveType type, boolean readFull) {
        this(id, type);
        this.readFull = readFull;
    }

    /**
     * Replies true if this reader also reads immediate children of referring primitives
     *
     * @return true if this reader also reads immediate children of referring primitives
     */
    public boolean isReadFull() {
        return readFull;
    }

    /**
     * Set true if this reader should reads immediate children of referring primitives too. False, otherwise.
     *
     * @param readFull true if this reader should reads immediate children of referring primitives too. False, otherwise.
     * @return {@code this}, for easy chaining
     * @since 15426
     */
    public OsmServerBackreferenceReader setReadFull(boolean readFull) {
        this.readFull = readFull;
        return this;
    }

    /**
     * Determines if this reader allows to return incomplete parent ways of a node.
     * @return {@code true} if this reader allows to return incomplete parent ways of a node
     * @since 15426
     */
    public boolean isAllowIncompleteParentWays() {
        return allowIncompleteParentWays;
    }

    /**
     * Sets whether this reader allows to return incomplete parent ways of a node.
     * @param allowIncompleteWays {@code true} if this reader allows to return incomplete parent ways of a node
     * @return {@code this}, for easy chaining
     * @since 15426
     */
    public OsmServerBackreferenceReader setAllowIncompleteParentWays(boolean allowIncompleteWays) {
        this.allowIncompleteParentWays = allowIncompleteWays;
        return this;
    }

    private DataSet getReferringPrimitives(ProgressMonitor progressMonitor, String type, String message) throws OsmTransferException {
        progressMonitor.beginTask(null, 2);
        try {
            progressMonitor.subTask(tr("Contacting OSM Server..."));
            StringBuilder sb = new StringBuilder();
            sb.append(primitiveType.getAPIName()).append('/').append(id).append(type);

            try (InputStream in = getInputStream(sb.toString(), progressMonitor.createSubTaskMonitor(1, true))) {
                if (in == null)
                    return null;
                progressMonitor.subTask(message);
                return OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, true));
            }
        } catch (OsmTransferException e) {
            throw e;
        } catch (IOException | IllegalDataException e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
        }
    }

    /**
     * Reads referring ways from the API server and replies them in a {@link DataSet}
     *
     * @param progressMonitor progress monitor
     * @return the data set
     * @throws OsmTransferException if any error occurs during dialog with OSM API
     */
    protected DataSet getReferringWays(ProgressMonitor progressMonitor) throws OsmTransferException {
        return getReferringPrimitives(progressMonitor, "/ways", tr("Downloading referring ways ..."));
    }

    /**
     * Reads referring relations from the API server and replies them in a {@link DataSet}
     *
     * @param progressMonitor the progress monitor
     * @return the data set
     * @throws OsmTransferException if any error occurs during dialog with OSM API
     */
    protected DataSet getReferringRelations(ProgressMonitor progressMonitor) throws OsmTransferException {
        return getReferringPrimitives(progressMonitor, "/relations", tr("Downloading referring relations ..."));
    }

    /**
     * Scans a dataset for incomplete primitives. Depending on the configuration of this reader
     * incomplete primitives are read from the server with an individual <code>/api/0.6/[way,relation]/#id/full</code>
     * request.
     *
     * <ul>
     *   <li>if this reader reads referrers for a {@link org.openstreetmap.josm.data.osm.Node}, referring ways are always
     *     read fully, unless {@link #setAllowIncompleteParentWays(boolean)} is set to true.</li>
     *   <li>if this reader reads referrers for an {@link Way} or a {@link Relation}, referring relations
     *    are only read fully if {@link #setReadFull(boolean)} is set to true.</li>
     * </ul>
     *
     * The method replies the modified dataset.
     *
     * @param ds the original dataset
     * @param progressMonitor  the progress monitor
     * @return the modified dataset
     * @throws OsmTransferException if an exception occurs.
     */
    protected DataSet readIncompletePrimitives(DataSet ds, ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(null, 2);
        try {
            Collection<Way> waysToCheck = new ArrayList<>(ds.getWays());
            if (isReadFull() || (primitiveType == OsmPrimitiveType.NODE && !isAllowIncompleteParentWays())) {
                for (Way way: waysToCheck) {
                    if (!way.isNew() && way.hasIncompleteNodes()) {
                        OsmServerObjectReader reader = new OsmServerObjectReader(way.getId(), OsmPrimitiveType.from(way), true /* read full */);
                        DataSet wayDs = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
                        DataSetMerger visitor = new DataSetMerger(ds, wayDs);
                        visitor.merge();
                    }
                }
            }
            if (isReadFull()) {
                Collection<Relation> relationsToCheck = new ArrayList<>(ds.getRelations());
                for (Relation relation: relationsToCheck) {
                    if (!relation.isNew() && relation.hasIncompleteMembers()) {
                        OsmServerObjectReader reader = new OsmServerObjectReader(relation.getId(), OsmPrimitiveType.from(relation), true);
                        DataSet wayDs = reader.parseOsm(progressMonitor.createSubTaskMonitor(1, false));
                        DataSetMerger visitor = new DataSetMerger(ds, wayDs);
                        visitor.merge();
                    }
                }
            }
            return ds;
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Reads the referring primitives from the OSM server, parses them and
     * replies them as {@link DataSet}
     *
     * @param progressMonitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null.
     * @return the dataset with the referring primitives
     * @throws OsmTransferException if an error occurs while communicating with the server
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        try {
            progressMonitor.beginTask(null, 3);
            DataSet ret = new DataSet();
            if (primitiveType == OsmPrimitiveType.NODE) {
                DataSet ds = getReferringWays(progressMonitor.createSubTaskMonitor(1, false));
                DataSetMerger visitor = new DataSetMerger(ret, ds);
                visitor.merge();
                ret = visitor.getTargetDataSet();
            }
            DataSet ds = getReferringRelations(progressMonitor.createSubTaskMonitor(1, false));
            DataSetMerger visitor = new DataSetMerger(ret, ds);
            visitor.merge();
            ret = visitor.getTargetDataSet();
            if (ret != null) {
                readIncompletePrimitives(ret, progressMonitor.createSubTaskMonitor(1, false));
                ret.deleteInvisible();
            }
            return ret;
        } finally {
            progressMonitor.finishTask();
        }
    }
}
