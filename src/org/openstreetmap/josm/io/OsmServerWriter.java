// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Class that uploads all changes to the osm server.
 *
 * This is done like this: - All objects with id = 0 are uploaded as new, except
 * those in deleted, which are ignored - All objects in deleted list are
 * deleted. - All remaining objects with modified flag set are updated.
 */
public class OsmServerWriter {
    /**
     * This list contains all successfully processed objects. The caller of
     * upload* has to check this after the call and update its dataset.
     *
     * If a server connection error occurs, this may contain fewer entries
     * than where passed in the list to upload*.
     */
    private Collection<OsmPrimitive> processed;

    private static volatile List<OsmServerWritePostprocessor> postprocessors;

    /**
     * Registers a post-processor.
     * @param pp post-processor to register
     */
    public static void registerPostprocessor(OsmServerWritePostprocessor pp) {
        if (postprocessors == null) {
            postprocessors = new ArrayList<>();
        }
        postprocessors.add(pp);
    }

    /**
     * Unregisters a post-processor.
     * @param pp post-processor to unregister
     */
    public static void unregisterPostprocessor(OsmServerWritePostprocessor pp) {
        if (postprocessors != null) {
            postprocessors.remove(pp);
        }
    }

    private final OsmApi api = OsmApi.getOsmApi();
    private boolean canceled;

    private long uploadStartTime;

    protected String timeLeft(int progress, int listSize) {
        long now = System.currentTimeMillis();
        long elapsed = now - uploadStartTime;
        if (elapsed == 0) {
            elapsed = 1;
        }
        double uploadsPerMs = (double) progress / elapsed;
        double uploadsLeft = (double) listSize - progress;
        long msLeft = (long) (uploadsLeft / uploadsPerMs);
        long minutesLeft = msLeft / TimeUnit.MINUTES.toMillis(1);
        long secondsLeft = (msLeft / TimeUnit.SECONDS.toMillis(1)) % TimeUnit.MINUTES.toSeconds(1);
        StringBuilder timeLeftStr = new StringBuilder().append(minutesLeft).append(':');
        if (secondsLeft < 10) {
            timeLeftStr.append('0');
        }
        return timeLeftStr.append(secondsLeft).toString();
    }

    /**
     * Uploads the changes individually. Invokes one API call per uploaded primitive.
     *
     * @param primitives the collection of primitives to upload
     * @param progressMonitor the progress monitor
     * @throws OsmTransferException if an exception occurs
     */
    protected void uploadChangesIndividually(Collection<? extends OsmPrimitive> primitives, ProgressMonitor progressMonitor)
            throws OsmTransferException {
        try {
            progressMonitor.beginTask(tr("Starting to upload with one request per primitive ..."));
            progressMonitor.setTicksCount(primitives.size());
            uploadStartTime = System.currentTimeMillis();
            for (OsmPrimitive osm : primitives) {
                String msg;
                switch(OsmPrimitiveType.from(osm)) {
                case NODE: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading node ''{4}'' (id: {5})"); break;
                case WAY: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading way ''{4}'' (id: {5})"); break;
                case RELATION: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading relation ''{4}'' (id: {5})"); break;
                default: throw new AssertionError();
                }
                int progress = progressMonitor.getTicks();
                progressMonitor.subTask(
                        tr(msg,
                                Math.round(100.0*progress/primitives.size()),
                                progress,
                                primitives.size(),
                                timeLeft(progress, primitives.size()),
                                osm.getName() == null ? osm.getId() : osm.getName(), osm.getId()));
                makeApiRequest(osm, progressMonitor);
                processed.add(osm);
                progressMonitor.worked(1);
            }
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Upload all changes in one diff upload
     *
     * @param primitives the collection of primitives to upload
     * @param progressMonitor  the progress monitor
     * @throws OsmTransferException if an exception occurs
     */
    protected void uploadChangesAsDiffUpload(Collection<? extends OsmPrimitive> primitives, ProgressMonitor progressMonitor)
            throws OsmTransferException {
        try {
            progressMonitor.beginTask(tr("Starting to upload in one request ..."));
            processed.addAll(api.uploadDiff(primitives, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false)));
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Upload all changes in one diff upload
     *
     * @param primitives the collection of primitives to upload
     * @param progressMonitor  the progress monitor
     * @param chunkSize the size of the individual upload chunks. &gt; 0 required.
     * @throws IllegalArgumentException if chunkSize &lt;= 0
     * @throws OsmTransferException if an exception occurs
     */
    protected void uploadChangesInChunks(Collection<? extends OsmPrimitive> primitives, ProgressMonitor progressMonitor, int chunkSize)
            throws OsmTransferException {
        if (chunkSize <= 0)
            throw new IllegalArgumentException(tr("Value >0 expected for parameter ''{0}'', got {1}", "chunkSize", chunkSize));
        try {
            progressMonitor.beginTask(tr("Starting to upload in chunks..."));
            List<OsmPrimitive> chunk = new ArrayList<>(chunkSize);
            Iterator<? extends OsmPrimitive> it = primitives.iterator();
            int numChunks = (int) Math.ceil((double) primitives.size() / (double) chunkSize);
            int i = 0;
            while (it.hasNext()) {
                i++;
                if (canceled) return;
                int j = 0;
                chunk.clear();
                while (it.hasNext() && j < chunkSize) {
                    if (canceled) return;
                    j++;
                    chunk.add(it.next());
                }
                progressMonitor.setCustomText(
                        trn("({0}/{1}) Uploading {2} object...",
                                "({0}/{1}) Uploading {2} objects...",
                                chunk.size(), i, numChunks, chunk.size()));
                processed.addAll(api.uploadDiff(chunk, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false)));
            }
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Send the dataset to the server.
     *
     * @param strategy the upload strategy. Must not be null.
     * @param primitives list of objects to send
     * @param changeset the changeset the data is uploaded to. Must not be null.
     * @param monitor the progress monitor. If null, assumes {@link NullProgressMonitor#INSTANCE}
     * @throws IllegalArgumentException if changeset is null
     * @throws IllegalArgumentException if strategy is null
     * @throws OsmTransferException if something goes wrong
     */
    public void uploadOsm(UploadStrategySpecification strategy, Collection<? extends OsmPrimitive> primitives,
            Changeset changeset, ProgressMonitor monitor) throws OsmTransferException {
        CheckParameterUtil.ensureParameterNotNull(changeset, "changeset");
        processed = new LinkedList<>();
        monitor = monitor == null ? NullProgressMonitor.INSTANCE : monitor;
        monitor.beginTask(tr("Uploading data ..."));
        try {
            api.initialize(monitor);
            // check whether we can use diff upload
            if (changeset.getId() == 0) {
                api.openChangeset(changeset, monitor.createSubTaskMonitor(0, false));
                // update the user information
                changeset.setUser(UserIdentityManager.getInstance().asUser());
            } else {
                api.updateChangeset(changeset, monitor.createSubTaskMonitor(0, false));
            }
            api.setChangeset(changeset);
            switch(strategy.getStrategy()) {
            case SINGLE_REQUEST_STRATEGY:
                uploadChangesAsDiffUpload(primitives, monitor.createSubTaskMonitor(0, false));
                break;
            case INDIVIDUAL_OBJECTS_STRATEGY:
                uploadChangesIndividually(primitives, monitor.createSubTaskMonitor(0, false));
                break;
            case CHUNKED_DATASET_STRATEGY:
            default:
                uploadChangesInChunks(primitives, monitor.createSubTaskMonitor(0, false), strategy.getChunkSize());
                break;
            }
        } finally {
            executePostprocessors(monitor);
            monitor.finishTask();
            api.setChangeset(null);
        }
    }

    void makeApiRequest(OsmPrimitive osm, ProgressMonitor progressMonitor) throws OsmTransferException {
        if (osm.isDeleted()) {
            api.deletePrimitive(osm, progressMonitor);
        } else if (osm.isNew()) {
            api.createPrimitive(osm, progressMonitor);
        } else {
            api.modifyPrimitive(osm, progressMonitor);
        }
    }

    /**
     * Cancel operation.
     */
    public void cancel() {
        this.canceled = true;
        if (api != null) {
            api.cancel();
        }
    }

    /**
     * Replies the collection of successfully processed primitives
     *
     * @return the collection of successfully processed primitives
     */
    public Collection<OsmPrimitive> getProcessedPrimitives() {
        return processed;
    }

    /**
     * Calls all registered upload postprocessors.
     * @param pm progress monitor
     */
    public void executePostprocessors(ProgressMonitor pm) {
        if (postprocessors != null) {
            for (OsmServerWritePostprocessor pp : postprocessors) {
                pp.postprocessUploadedPrimitives(processed, pm);
            }
        }
    }
}
