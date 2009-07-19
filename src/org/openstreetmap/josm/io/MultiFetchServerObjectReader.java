// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Retrieves a set of {@see OsmPrimitive}s from an OSM server using the so called
 * Multi Fetch API.
 *
 * Usage:
 * <pre>
 *    MultiFetchServerObjectReader reader = MultiFetchServerObjectReader()
 *         .append(2345,2334,4444)
 *         .append(new Node(72343));
 *    reader.parseOsm();
 *    if (!reader.getMissingPrimitives().isEmpty()) {
 *        System.out.println("There are missing primitives: " + reader.getMissingPrimitives());
 *    }
 *    if (!reader.getSkippedWays().isEmpty()) {
 *       System.out.println("There are skipped ways: " + reader.getMissingPrimitives());
 *    }
 * </pre>
 *
 *
 */
public class MultiFetchServerObjectReader extends OsmServerReader{

    static private Logger logger = Logger.getLogger(MultiFetchServerObjectReader.class.getName());
    /**
     * the max. number of primitives retrieved in one step. Assuming IDs with 7 digits,
     * this leads to a max. request URL of ~  1600 Bytes ((7 digits +  1 Seperator) * 200),
     * which should be safe according to the
     * <a href="http://www.boutell.com/newfaq/misc/urllength.html">WWW FAQ</a>.
     *
     */
    static private int MAX_IDS_PER_REQUEST = 200;


    private HashSet<Long> nodes;
    private HashSet<Long> ways;
    private HashSet<Long> relations;
    private HashSet<Long> missingPrimitives;
    private DataSet outputDataSet;

    /**
     * constructor
     *
     */
    public MultiFetchServerObjectReader() {
        nodes = new HashSet<Long>();
        ways = new HashSet<Long>();
        relations = new HashSet<Long>();
        this.outputDataSet = new DataSet();
        this.missingPrimitives = new HashSet<Long>();
    }

    /**
     * remembers an {@see OsmPrimitive}'s id and its type. The id will
     * later be fetched as part of a Multi Get request.
     *
     * Ignore the id if it id <= 0.
     *
     * @param id  the id
     * @param type  the type
     */
    protected void remember(long id, OsmPrimitiveType type) {
        if (id <= 0) return;
        if (type.equals(OsmPrimitiveType.NODE)) {
            nodes.add(id);
        } else if (type.equals(OsmPrimitiveType.WAY)) {
            ways.add(id);
        } if (type.equals(OsmPrimitiveType.RELATION)) {
            relations.add(id);
        }
    }

    /**
     * remembers an {@see OsmPrimitive}'s id. <code>ds</code> must include
     * an {@see OsmPrimitive} with id=<code>id</code>. The id will
     * later we fetched as part of a Multi Get request.
     *
     * Ignore the id if it id <= 0.
     *
     * @param ds  the dataset (must not be null)
     * @param id  the id
     * @exception IllegalArgumentException thrown, if ds is null
     * @exception NoSuchElementException thrown, if ds doesn't include an {@see OsmPrimitive} with
     *   id=<code>id</code>
     */
    protected void remember(DataSet ds, long id) throws IllegalArgumentException, NoSuchElementException{
        if (ds == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "ds"));
        if (id <= 0) return;
        OsmPrimitive primitive = ds.getPrimitiveById(id);
        if (primitive == null)
            throw new NoSuchElementException(tr("no primitive with id {0} in local dataset. Can't infer primitive type", id));
        remember(id, OsmPrimitiveType.from(primitive));
        return;
    }

    /**
     * appends a list of  ids to the list of ids which will be fetched from the server. ds must
     * include an {@see OsmPrimitive} for each id in ids.
     *
     * id is ignored if id <= 0.
     *
     * @param ds  the dataset
     * @param ids  the list of ids
     * @return this
     *
     */
    public MultiFetchServerObjectReader append(DataSet ds, long ... ids)  {
        if (ids == null) return this;
        for (int i=0; i < ids.length; i++) {
            remember(ds, ids[i]);
        }
        return this;
    }

    /**
     * appends a collection of  ids to the list of ids which will be fetched from the server. ds must
     * include an {@see OsmPrimitive} for each id in ids.
     *
     * id is ignored if id <= 0.
     *
     * @param ds  the dataset
     * @param ids  the collection of ids
     * @return this
     *
     */
    public MultiFetchServerObjectReader append(DataSet ds, Collection<Long> ids) {
        if (ids == null) return null;
        for (long id: ids) {
            append(ds,id);
        }
        return this;
    }

    /**
     * appends a {@see Node}s id to the list of ids which will be fetched from the server.
     *
     * @param node  the node (ignored, if null)
     * @return this
     *
     */
    public MultiFetchServerObjectReader append(Node node) {
        if (node == null) return this;
        if (node.id == 0) return this;
        remember(node.id, OsmPrimitiveType.NODE);
        return this;
    }

    /**
     * appends a {@see Way}s id and the list of ids of nodes the way refers to to the list of ids which will be fetched from the server.
     *
     * @param way the way (ignored, if null)
     * @return this
     *
     */
    public MultiFetchServerObjectReader append(Way way) {
        if (way == null) return this;
        if (way.id == 0) return this;
        for (Node node: way.nodes) {
            if (node.id > 0) {
                remember(node.id, OsmPrimitiveType.NODE);
            }
        }
        remember(way.id, OsmPrimitiveType.WAY);
        return this;
    }

    /**
     * appends a {@see Relation}s id to the list of ids which will be fetched from the server.
     *
     * @param relation  the relation (ignored, if null)
     * @return this
     *
     */
    public MultiFetchServerObjectReader append(Relation relation) {
        if (relation == null) return this;
        if (relation.id == 0) return this;
        remember(relation.id, OsmPrimitiveType.RELATION);
        for (RelationMember member : relation.members) {
            appendGeneric(member.member);
        }
        return this;
    }


    protected MultiFetchServerObjectReader appendGeneric(OsmPrimitive primitive) {
        if (OsmPrimitiveType.from(primitive).equals(OsmPrimitiveType.NODE))
            return append((Node)primitive);
        else if (OsmPrimitiveType.from(primitive).equals(OsmPrimitiveType.WAY))
            return append((Way)primitive);
        else if (OsmPrimitiveType.from(primitive).equals(OsmPrimitiveType.RELATION))
            return append((Relation)primitive);

        return this;
    }

    /**
     * appends a list of {@see OsmPrimitive} to the list of ids which will be fetched from the server.
     *
     * @param primitives  the list of primitives (ignored, if null)
     * @return this
     *
     * @see #append(Node)
     * @see #append(Way)
     * @see #append(Relation)
     *
     */
    public MultiFetchServerObjectReader append(Collection<OsmPrimitive> primitives) {
        if (primitives == null) return this;
        for (OsmPrimitive primitive : primitives) {
            appendGeneric(primitive);
        }
        return this;
    }

    /**
     * extracts a subset of max {@see #MAX_IDS_PER_REQUEST} ids from <code>ids</code> and
     * replies the subset. The extracted subset is removed from <code>ids</code>.
     *
     * @param ids a set of ids
     * @return the subset of ids
     */
    protected Set<Long> extractIdPackage(Set<Long> ids) {
        HashSet<Long> pkg = new HashSet<Long>();
        if (ids.isEmpty())
            return pkg;
        if (ids.size() > MAX_IDS_PER_REQUEST) {
            Iterator<Long> it = ids.iterator();
            for (int i =0;i<MAX_IDS_PER_REQUEST;i++) {
                pkg.add(it.next());
            }
            ids.removeAll(pkg);
        } else {
            pkg.addAll(ids);
            ids.clear();
        }
        return pkg;
    }


    /**
     * builds the Multi Get request string for a set of ids and a given
     * {@see OsmPrimitiveType}.
     *
     * @param type the type
     * @param idPackage  the package of ids
     * @return the request string
     */
    protected String buildRequestString(OsmPrimitiveType type, Set<Long> idPackage) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getAPIName()).append("s?")
        .append(type.getAPIName()).append("s=");

        Iterator<Long> it = idPackage.iterator();
        for (int i=0; i< idPackage.size();i++) {
            sb.append(it.next());
            if (i < idPackage.size()-1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * builds the Multi Get request string for a single id and a given
     * {@see OsmPrimitiveType}.
     *
     * @param type the type
     * @param id the id
     * @return the request string
     */
    protected String buildRequestString(OsmPrimitiveType type, long id) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getAPIName()).append("s?")
        .append(type.getAPIName()).append("s=")
        .append(id);
        return sb.toString();
    }

    /**
     * invokes a Multi Get for a set of ids and a given {@see OsmPrimitiveType}.
     * The retrieved primitives are merged to {@see #outputDataSet}.
     *
     * @param type the type
     * @param pkg the package of ids
     * @exception OsmTransferException thrown if an error occurs while communicating with the API server
     *
     */
    protected void multiGetIdPackage(OsmPrimitiveType type, Set<Long> pkg, ProgressMonitor progressMonitor) throws OsmTransferException {
        String request = buildRequestString(type, pkg);
        final InputStream in = getInputStream(request, NullProgressMonitor.INSTANCE);
        if (in == null) return;
        progressMonitor.subTask(tr("Downloading OSM data..."));
        try {
            final OsmReader osm = OsmReader.parseDataSetOsm(in, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            merge(osm.getDs());
        } catch(Exception e) {
            throw new OsmTransferException(e);
        }
    }

    /**
     * invokes a Multi Get for a single id and a given {@see OsmPrimitiveType}.
     * The retrieved primitive is merged to {@see #outputDataSet}.
     *
     * @param type the type
     * @param id the id
     * @exception OsmTransferException thrown if an error occurs while communicating with the API server
     *
     */
    protected void singleGetId(OsmPrimitiveType type, long id, ProgressMonitor progressMonitor) throws OsmTransferException {
        String request = buildRequestString(type, id);
        final InputStream in = getInputStream(request, NullProgressMonitor.INSTANCE);
        if (in == null)
            return;
        progressMonitor.subTask(tr("Downloading OSM data..."));
        try {
            final OsmReader osm = OsmReader.parseDataSetOsm(in, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            merge(osm.getDs());
        } catch(Exception e) {
            throw new OsmTransferException(e);
        }
    }

    /**
     * invokes a sequence of Multi Gets for individual ids in a set of ids and a given {@see OsmPrimitiveType}.
     * The retrieved primitives are merged to {@see #outputDataSet}.
     *
     * This method is used if one of the ids in pkg doesn't exist (the server replies with return code 404).
     * If the set is fetched with this method it is possible to find out which of the ids doesn't exist.
     * Unfortunatelly, the server does not provide an error header or an error body for a 404 reply.
     *
     * @param type the type
     * @param pkg the set of ids
     * @exception OsmTransferException thrown if an error occurs while communicating with the API server
     *
     */
    protected void singleGetIdPackage(OsmPrimitiveType type, Set<Long> pkg, ProgressMonitor progressMonitor) throws OsmTransferException {
        for (long id : pkg) {
            try {
                singleGetId(type, id, progressMonitor);
            } catch(OsmApiException e) {
                if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    logger.warning(tr("Server replied with response code 404 for id {0}. Skipping.", Long.toString(id)));
                    missingPrimitives.add(id);
                    continue;
                }
                throw e;
            }
        }
    }

    /**
     * merges the dataset <code>from</code> to {@see #outputDataSet}.
     *
     * @param from the other dataset
     *
     */
    protected void merge(DataSet from) {
        final MergeVisitor visitor = new MergeVisitor(outputDataSet,from);
        visitor.merge();
    }

    /**
     * fetches a set of ids of a given {@see OsmPrimitiveType} from the server
     *
     * @param ids the set of ids
     * @param type the  type
     * @exception OsmTransferException thrown if an error occurs while communicating with the API server
     */
    protected void fetchPrimitives(Set<Long> ids, OsmPrimitiveType type, ProgressMonitor progressMonitor) throws OsmTransferException{
        Set<Long> toFetch = new HashSet<Long>(ids);
        toFetch.addAll(ids);
        while(! toFetch.isEmpty()) {
            Set<Long> pkg = extractIdPackage(toFetch);
            try {
                multiGetIdPackage(type, pkg, progressMonitor);
            } catch(OsmApiException e) {
                if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    logger.warning(tr("Server replied with response code 404, retrying with an individual request for each primitive"));
                    singleGetIdPackage(type, pkg, progressMonitor);
                } else
                    throw e;
            }
        }
    }

    /**
     * invokes one or more Multi Gets to fetch the {@see OsmPrimitive}s and replies
     * the dataset of retrieved primitives. Note that the dataset includes non visible primitives too!
     * In contrast to a simple Get for a node, a way, or a relation, a Multi Get always replies
     * the latest version of the primitive (if any), even if the primitive is not visible (i.e. if
     * visible==false).
     *
     * Invoke {@see #getMissingPrimitives()} to get a list of primitives which have not been
     * found on  the server (the server response code was 404)
     *
     * Invoke {@see #getSkippedWay()} to get a list of ways which this reader could not build from
     * the fetched data because the ways refer to nodes which don't exist on the server.
     *
     * @return the parsed data
     * @exception OsmTransferException thrown if an error occurs while communicating with the API server
     * @see #getMissingPrimitives()
     * @see #getSkippedWays()
     *

     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask("");
        try {
            missingPrimitives = new HashSet<Long>();

            fetchPrimitives(nodes,OsmPrimitiveType.NODE, progressMonitor);
            fetchPrimitives(ways,OsmPrimitiveType.WAY, progressMonitor);
            fetchPrimitives(relations,OsmPrimitiveType.RELATION, progressMonitor);
            return outputDataSet;
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * replies the set of ids of all primitives for which a fetch request to the
     * server was submitted but which are not available from the server (the server
     * replied a return code of 404)
     *
     * @return the set of ids of missing primitives
     */
    public Set<Long> getMissingPrimitives() {
        return missingPrimitives;
    }
}
