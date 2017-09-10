// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Retrieves a set of {@link OsmPrimitive}s from an OSM server using the so called
 * Multi Fetch API.
 *
 * Usage:
 * <pre>
 *    MultiFetchServerObjectReader reader = MultiFetchServerObjectReader()
 *         .append(2345,2334,4444)
 *         .append(new Node(72343));
 *    reader.parseOsm();
 *    if (!reader.getMissingPrimitives().isEmpty()) {
 *        Logging.info("There are missing primitives: " + reader.getMissingPrimitives());
 *    }
 *    if (!reader.getSkippedWays().isEmpty()) {
 *       Logging.info("There are skipped ways: " + reader.getMissingPrimitives());
 *    }
 * </pre>
 */
public class MultiFetchServerObjectReader extends OsmServerReader {
    /**
     * the max. number of primitives retrieved in one step. Assuming IDs with 7 digits,
     * this leads to a max. request URL of ~ 1600 Bytes ((7 digits +  1 Separator) * 200),
     * which should be safe according to the
     * <a href="http://www.boutell.com/newfaq/misc/urllength.html">WWW FAQ</a>.
     */
    private static final int MAX_IDS_PER_REQUEST = 200;

    private final Set<Long> nodes;
    private final Set<Long> ways;
    private final Set<Long> relations;
    private Set<PrimitiveId> missingPrimitives;
    private final DataSet outputDataSet;

    /**
     * Constructs a {@code MultiFetchServerObjectReader}.
     */
    protected MultiFetchServerObjectReader() {
        nodes = new LinkedHashSet<>();
        ways = new LinkedHashSet<>();
        relations = new LinkedHashSet<>();
        this.outputDataSet = new DataSet();
        this.missingPrimitives = new LinkedHashSet<>();
    }

    /**
     * Creates a new instance of {@link MultiFetchServerObjectReader} or {@link MultiFetchOverpassObjectReader}
     * depending on the {@link OverpassDownloadReader#FOR_MULTI_FETCH preference}.
     *
     * @return a new instance
     * @since 9241
     */
    public static MultiFetchServerObjectReader create() {
        return create(OverpassDownloadReader.FOR_MULTI_FETCH.get());
    }

    /**
     * Creates a new instance of {@link MultiFetchServerObjectReader} or {@link MultiFetchOverpassObjectReader}
     * depending on the {@code fromMirror} parameter.
     *
     * @param fromMirror {@code false} for {@link MultiFetchServerObjectReader}, {@code true} for {@link MultiFetchOverpassObjectReader}
     * @return a new instance
     * @since 9241
     */
    static MultiFetchServerObjectReader create(final boolean fromMirror) {
        if (fromMirror) {
            return new MultiFetchOverpassObjectReader();
        } else {
            return new MultiFetchServerObjectReader();
        }
    }

    /**
     * Remembers an {@link OsmPrimitive}'s id. The id will
     * later be fetched as part of a Multi Get request.
     *
     * Ignore the id if it represents a new primitives.
     *
     * @param id  the id
     */
    protected void remember(PrimitiveId id) {
        if (id.isNew()) return;
        switch(id.getType()) {
        case NODE: nodes.add(id.getUniqueId()); break;
        case WAY: ways.add(id.getUniqueId()); break;
        case RELATION: relations.add(id.getUniqueId()); break;
        default: throw new AssertionError();
        }
    }

    /**
     * appends a {@link OsmPrimitive} id to the list of ids which will be fetched from the server.
     *
     * @param ds the {@link DataSet} to which the primitive belongs
     * @param id the primitive id
     * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE}, {@link OsmPrimitiveType#WAY WAY},
     * {@link OsmPrimitiveType#RELATION RELATION}
     * @return this
     */
    public MultiFetchServerObjectReader append(DataSet ds, long id, OsmPrimitiveType type) {
        OsmPrimitive p = ds.getPrimitiveById(id, type);
        switch(type) {
        case NODE:
            return appendNode((Node) p);
        case WAY:
            return appendWay((Way) p);
        case RELATION:
            return appendRelation((Relation) p);
        default:
            return this;
        }
    }

    /**
     * appends a {@link Node} id to the list of ids which will be fetched from the server.
     *
     * @param node  the node (ignored, if null)
     * @return this
     */
    public MultiFetchServerObjectReader appendNode(Node node) {
        if (node == null) return this;
        remember(node.getPrimitiveId());
        return this;
    }

    /**
     * appends a {@link Way} id and the list of ids of nodes the way refers to the list of ids which will be fetched from the server.
     *
     * @param way the way (ignored, if null)
     * @return this
     */
    public MultiFetchServerObjectReader appendWay(Way way) {
        if (way == null) return this;
        if (way.isNew()) return this;
        for (Node node: !recursesDown() ? way.getNodes() : Collections.<Node>emptyList()) {
            if (!node.isNew()) {
                remember(node.getPrimitiveId());
            }
        }
        remember(way.getPrimitiveId());
        return this;
    }

    /**
     * appends a {@link Relation} id to the list of ids which will be fetched from the server.
     *
     * @param relation  the relation (ignored, if null)
     * @return this
     */
    protected MultiFetchServerObjectReader appendRelation(Relation relation) {
        if (relation == null) return this;
        if (relation.isNew()) return this;
        remember(relation.getPrimitiveId());
        for (RelationMember member : !recursesDown() ? relation.getMembers() : Collections.<RelationMember>emptyList()) {
            // avoid infinite recursion in case of cyclic dependencies in relations
            if (OsmPrimitiveType.from(member.getMember()).equals(OsmPrimitiveType.RELATION)
                    && relations.contains(member.getMember().getId())) {
                continue;
            }
            if (!member.getMember().isIncomplete()) {
                append(member.getMember());
            }
        }
        return this;
    }

    /**
     * appends an {@link OsmPrimitive} to the list of ids which will be fetched from the server.
     * @param primitive the primitive
     * @return this
     */
    public MultiFetchServerObjectReader append(OsmPrimitive primitive) {
        if (primitive instanceof Node) {
            return appendNode((Node) primitive);
        } else if (primitive instanceof Way) {
            return appendWay((Way) primitive);
        } else if (primitive instanceof Relation) {
            return appendRelation((Relation) primitive);
        }
        return this;
    }

    /**
     * appends a list of {@link OsmPrimitive} to the list of ids which will be fetched from the server.
     *
     * @param primitives  the list of primitives (ignored, if null)
     * @return this
     *
     * @see #append(OsmPrimitive)
     */
    public MultiFetchServerObjectReader append(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null) return this;
        for (OsmPrimitive primitive : primitives) {
            append(primitive);
        }
        return this;
    }

    /**
     * extracts a subset of max {@link #MAX_IDS_PER_REQUEST} ids from <code>ids</code> and
     * replies the subset. The extracted subset is removed from <code>ids</code>.
     *
     * @param ids a set of ids
     * @return the subset of ids
     */
    protected Set<Long> extractIdPackage(Set<Long> ids) {
        Set<Long> pkg = new HashSet<>();
        if (ids.isEmpty())
            return pkg;
        if (ids.size() > MAX_IDS_PER_REQUEST) {
            Iterator<Long> it = ids.iterator();
            for (int i = 0; i < MAX_IDS_PER_REQUEST; i++) {
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
     * builds the Multi Get request string for a set of ids and a given {@link OsmPrimitiveType}.
     *
     * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE}, {@link OsmPrimitiveType#WAY WAY},
     * {@link OsmPrimitiveType#RELATION RELATION}
     * @param idPackage  the package of ids
     * @return the request string
     */
    protected String buildRequestString(final OsmPrimitiveType type, Set<Long> idPackage) {
        return type.getAPIName() + "s?" + type.getAPIName() + "s=" + Utils.join(",", idPackage);
    }

    protected void rememberNodesOfIncompleteWaysToLoad(DataSet from) {
        for (Way w: from.getWays()) {
            if (w.hasIncompleteNodes()) {
                for (Node n: w.getNodes()) {
                    if (n.isIncomplete()) {
                        nodes.add(n.getId());
                    }
                }
            }
        }
    }

    /**
     * merges the dataset <code>from</code> to {@link #outputDataSet}.
     *
     * @param from the other dataset
     */
    protected void merge(DataSet from) {
        final DataSetMerger visitor = new DataSetMerger(outputDataSet, from);
        visitor.merge();
    }

    /**
     * fetches a set of ids of a given {@link OsmPrimitiveType} from the server
     *
     * @param ids the set of ids
     * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE}, {@link OsmPrimitiveType#WAY WAY},
     * {@link OsmPrimitiveType#RELATION RELATION}
     * @param progressMonitor progress monitor
     * @throws OsmTransferException if an error occurs while communicating with the API server
     */
    protected void fetchPrimitives(Set<Long> ids, OsmPrimitiveType type, ProgressMonitor progressMonitor) throws OsmTransferException {
        String msg;
        final String baseUrl = getBaseUrl();
        switch (type) {
            // CHECKSTYLE.OFF: SingleSpaceSeparator
            case NODE:     msg = tr("Fetching a package of nodes from ''{0}''",     baseUrl); break;
            case WAY:      msg = tr("Fetching a package of ways from ''{0}''",      baseUrl); break;
            case RELATION: msg = tr("Fetching a package of relations from ''{0}''", baseUrl); break;
            // CHECKSTYLE.ON: SingleSpaceSeparator
            default: throw new AssertionError();
        }
        progressMonitor.setTicksCount(ids.size());
        progressMonitor.setTicks(0);
        // The complete set containing all primitives to fetch
        Set<Long> toFetch = new HashSet<>(ids);
        // Build a list of fetchers that will  download smaller sets containing only MAX_IDS_PER_REQUEST (200) primitives each.
        // we will run up to MAX_DOWNLOAD_THREADS concurrent fetchers.
        int threadsNumber = Main.pref.getInteger("osm.download.threads", OsmApi.MAX_DOWNLOAD_THREADS);
        threadsNumber = Utils.clamp(threadsNumber, 1, OsmApi.MAX_DOWNLOAD_THREADS);
        final ExecutorService exec = Executors.newFixedThreadPool(
                threadsNumber, Utils.newThreadFactory(getClass() + "-%d", Thread.NORM_PRIORITY));
        CompletionService<FetchResult> ecs = new ExecutorCompletionService<>(exec);
        List<Future<FetchResult>> jobs = new ArrayList<>();
        while (!toFetch.isEmpty()) {
            jobs.add(ecs.submit(new Fetcher(type, extractIdPackage(toFetch), progressMonitor)));
        }
        // Run the fetchers
        for (int i = 0; i < jobs.size() && !isCanceled(); i++) {
            progressMonitor.subTask(msg + "... " + progressMonitor.getTicks() + '/' + progressMonitor.getTicksCount());
            try {
                FetchResult result = ecs.take().get();
                if (result.missingPrimitives != null) {
                    missingPrimitives.addAll(result.missingPrimitives);
                }
                if (result.dataSet != null && !isCanceled()) {
                    rememberNodesOfIncompleteWaysToLoad(result.dataSet);
                    merge(result.dataSet);
                }
            } catch (InterruptedException | ExecutionException e) {
                Logging.error(e);
            }
        }
        exec.shutdown();
        // Cancel requests if the user chose to
        if (isCanceled()) {
            for (Future<FetchResult> job : jobs) {
                job.cancel(true);
            }
        }
    }

    /**
     * invokes one or more Multi Gets to fetch the {@link OsmPrimitive}s and replies
     * the dataset of retrieved primitives. Note that the dataset includes non visible primitives too!
     * In contrast to a simple Get for a node, a way, or a relation, a Multi Get always replies
     * the latest version of the primitive (if any), even if the primitive is not visible (i.e. if
     * visible==false).
     *
     * Invoke {@link #getMissingPrimitives()} to get a list of primitives which have not been
     * found on  the server (the server response code was 404)
     *
     * @return the parsed data
     * @throws OsmTransferException if an error occurs while communicating with the API server
     * @see #getMissingPrimitives()
     *
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        int n = nodes.size() + ways.size() + relations.size();
        progressMonitor.beginTask(trn("Downloading {0} object from ''{1}''",
                "Downloading {0} objects from ''{1}''", n, n, OsmApi.getOsmApi().getBaseUrl()));
        try {
            missingPrimitives = new HashSet<>();
            if (isCanceled()) return null;
            fetchPrimitives(ways, OsmPrimitiveType.WAY, progressMonitor);
            if (isCanceled()) return null;
            fetchPrimitives(nodes, OsmPrimitiveType.NODE, progressMonitor);
            if (isCanceled()) return null;
            fetchPrimitives(relations, OsmPrimitiveType.RELATION, progressMonitor);
            if (outputDataSet != null) {
                outputDataSet.deleteInvisible();
            }
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
    public Set<PrimitiveId> getMissingPrimitives() {
        return missingPrimitives;
    }

    /**
     * Whether this reader fetches nodes when loading ways, or members when loading relations.
     *
     * @return {@code true} if the reader recurses down
     */
    protected boolean recursesDown() {
        return false;
    }

    /**
     * The class holding the results given by {@link Fetcher}.
     * It is only a wrapper of the resulting {@link DataSet} and the collection of {@link PrimitiveId} that could not have been loaded.
     */
    protected static class FetchResult {

        /**
         * The resulting data set
         */
        public final DataSet dataSet;

        /**
         * The collection of primitive ids that could not have been loaded
         */
        public final Set<PrimitiveId> missingPrimitives;

        /**
         * Constructs a {@code FetchResult}
         * @param dataSet The resulting data set
         * @param missingPrimitives The collection of primitive ids that could not have been loaded
         */
        public FetchResult(DataSet dataSet, Set<PrimitiveId> missingPrimitives) {
            this.dataSet = dataSet;
            this.missingPrimitives = missingPrimitives;
        }
    }

    /**
     * The class that actually download data from OSM API.
     * Several instances of this class are used by {@link MultiFetchServerObjectReader} (one per set of primitives to fetch).
     * The inheritance of {@link OsmServerReader} is only explained by the need to have a distinct OSM connection by {@code Fetcher} instance.
     * @see FetchResult
     */
    protected class Fetcher extends OsmServerReader implements Callable<FetchResult> {

        private final Set<Long> pkg;
        private final OsmPrimitiveType type;
        private final ProgressMonitor progressMonitor;

        /**
         * Constructs a {@code Fetcher}
         * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE}, {@link OsmPrimitiveType#WAY WAY},
         * {@link OsmPrimitiveType#RELATION RELATION}
         * @param idsPackage The set of primitives ids to fetch
         * @param progressMonitor The progress monitor
         */
        public Fetcher(OsmPrimitiveType type, Set<Long> idsPackage, ProgressMonitor progressMonitor) {
            this.pkg = idsPackage;
            this.type = type;
            this.progressMonitor = progressMonitor;
        }

        @Override
        public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
            // This method is implemented because of the OsmServerReader inheritance, but not used,
            // as the main target of this class is the call() method.
            return fetch(progressMonitor).dataSet;
        }

        @Override
        public FetchResult call() throws Exception {
            return fetch(progressMonitor);
        }

        /**
         * fetches the requested primitives and updates the specified progress monitor.
         * @param progressMonitor the progress monitor
         * @return the {@link FetchResult} of this operation
         * @throws OsmTransferException if an error occurs while communicating with the API server
         */
        protected FetchResult fetch(ProgressMonitor progressMonitor) throws OsmTransferException {
            try {
                return multiGetIdPackage(type, pkg, progressMonitor);
            } catch (OsmApiException e) {
                if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    Logging.info(tr("Server replied with response code 404, retrying with an individual request for each object."));
                    return singleGetIdPackage(type, pkg, progressMonitor);
                } else {
                    throw e;
                }
            }
        }

        @Override
        protected String getBaseUrl() {
            return MultiFetchServerObjectReader.this.getBaseUrl();
        }

        /**
         * invokes a Multi Get for a set of ids and a given {@link OsmPrimitiveType}.
         * The retrieved primitives are merged to {@link #outputDataSet}.
         *
         * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE}, {@link OsmPrimitiveType#WAY WAY},
         * {@link OsmPrimitiveType#RELATION RELATION}
         * @param pkg the package of ids
         * @param progressMonitor progress monitor
         * @return the {@link FetchResult} of this operation
         * @throws OsmTransferException if an error occurs while communicating with the API server
         */
        protected FetchResult multiGetIdPackage(OsmPrimitiveType type, Set<Long> pkg, ProgressMonitor progressMonitor)
                throws OsmTransferException {
            String request = buildRequestString(type, pkg);
            FetchResult result = null;
            try (InputStream in = getInputStream(request, NullProgressMonitor.INSTANCE)) {
                if (in == null) return null;
                progressMonitor.subTask(tr("Downloading OSM data..."));
                try {
                    result = new FetchResult(OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(pkg.size(), false)), null);
                } catch (IllegalDataException e) {
                    throw new OsmTransferException(e);
                }
            } catch (IOException ex) {
                Logging.warn(ex);
            }
            return result;
        }

        /**
         * invokes a Multi Get for a single id and a given {@link OsmPrimitiveType}.
         * The retrieved primitive is merged to {@link #outputDataSet}.
         *
         * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE}, {@link OsmPrimitiveType#WAY WAY},
         * {@link OsmPrimitiveType#RELATION RELATION}
         * @param id the id
         * @param progressMonitor progress monitor
         * @return the {@link DataSet} resulting of this operation
         * @throws OsmTransferException if an error occurs while communicating with the API server
         */
        protected DataSet singleGetId(OsmPrimitiveType type, long id, ProgressMonitor progressMonitor) throws OsmTransferException {
            String request = buildRequestString(type, Collections.singleton(id));
            DataSet result = null;
            try (InputStream in = getInputStream(request, NullProgressMonitor.INSTANCE)) {
                if (in == null) return null;
                progressMonitor.subTask(tr("Downloading OSM data..."));
                try {
                    result = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                } catch (IllegalDataException e) {
                    throw new OsmTransferException(e);
                }
            } catch (IOException ex) {
                Logging.warn(ex);
            }
            return result;
        }

        /**
         * invokes a sequence of Multi Gets for individual ids in a set of ids and a given {@link OsmPrimitiveType}.
         * The retrieved primitives are merged to {@link #outputDataSet}.
         *
         * This method is used if one of the ids in pkg doesn't exist (the server replies with return code 404).
         * If the set is fetched with this method it is possible to find out which of the ids doesn't exist.
         * Unfortunately, the server does not provide an error header or an error body for a 404 reply.
         *
         * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE}, {@link OsmPrimitiveType#WAY WAY},
         * {@link OsmPrimitiveType#RELATION RELATION}
         * @param pkg the set of ids
         * @param progressMonitor progress monitor
         * @return the {@link FetchResult} of this operation
         * @throws OsmTransferException if an error occurs while communicating with the API server
         */
        protected FetchResult singleGetIdPackage(OsmPrimitiveType type, Set<Long> pkg, ProgressMonitor progressMonitor)
                throws OsmTransferException {
            FetchResult result = new FetchResult(new DataSet(), new HashSet<PrimitiveId>());
            String baseUrl = OsmApi.getOsmApi().getBaseUrl();
            for (long id : pkg) {
                try {
                    String msg;
                    switch (type) {
                        // CHECKSTYLE.OFF: SingleSpaceSeparator
                        case NODE:     msg = tr("Fetching node with id {0} from ''{1}''",     id, baseUrl); break;
                        case WAY:      msg = tr("Fetching way with id {0} from ''{1}''",      id, baseUrl); break;
                        case RELATION: msg = tr("Fetching relation with id {0} from ''{1}''", id, baseUrl); break;
                        // CHECKSTYLE.ON: SingleSpaceSeparator
                        default: throw new AssertionError();
                    }
                    progressMonitor.setCustomText(msg);
                    result.dataSet.mergeFrom(singleGetId(type, id, progressMonitor));
                } catch (OsmApiException e) {
                    if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        Logging.info(tr("Server replied with response code 404 for id {0}. Skipping.", Long.toString(id)));
                        result.missingPrimitives.add(new SimplePrimitiveId(id, type));
                    } else {
                        throw e;
                    }
                }
            }
            return result;
        }
    }
}
