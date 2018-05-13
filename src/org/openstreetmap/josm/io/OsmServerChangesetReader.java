// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.XmlParsingException;

/**
 * Reads the history of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive} from the OSM API server.
 *
 */
public class OsmServerChangesetReader extends OsmServerReader {

    /**
     * don't use - not implemented!
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    protected final InputStream getChangesetInputStream(long id, boolean includeDiscussion, ProgressMonitor monitor)
            throws OsmTransferException {
        StringBuilder sb = new StringBuilder(48).append("changeset/").append(id);
        if (includeDiscussion) {
            sb.append("?include_discussion=true");
        }
        return getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
    }

    /**
     * Queries a list
     * @param query  the query specification. Must not be null.
     * @param monitor a progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @return the list of changesets read from the server
     * @throws IllegalArgumentException if query is null
     * @throws OsmTransferException if something goes wrong
     */
    public List<Changeset> queryChangesets(ChangesetQuery query, ProgressMonitor monitor) throws OsmTransferException {
        CheckParameterUtil.ensureParameterNotNull(query, "query");
        List<Changeset> result = null;
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Reading changesets..."));
            StringBuilder sb = new StringBuilder();
            sb.append("changesets?").append(query.getQueryString());
            try (InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true))) {
                if (in == null)
                    return Collections.emptyList();
                monitor.indeterminateSubTask(tr("Downloading changesets ..."));
                result = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
            } catch (IOException e) {
                Logging.warn(e);
            }
        } catch (OsmTransferException e) {
            throw e;
        } catch (IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
        return result;
    }

    /**
     * Reads the changeset with id <code>id</code> from the server.
     *
     * @param id the changeset id. id &gt; 0 required.
     * @param includeDiscussion determines if discussion comments must be downloaded or not
     * @param monitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @return the changeset read
     * @throws OsmTransferException if something goes wrong
     * @throws IllegalArgumentException if id &lt;= 0
     * @since 7704
     */
    public Changeset readChangeset(long id, boolean includeDiscussion, ProgressMonitor monitor) throws OsmTransferException {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got ''{1}''.", "id", id));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        Changeset result = null;
        try {
            monitor.beginTask(tr("Reading changeset {0} ...", id));
            try (InputStream in = getChangesetInputStream(id, includeDiscussion, monitor)) {
                if (in == null)
                    return null;
                monitor.indeterminateSubTask(tr("Downloading changeset {0} ...", id));
                List<Changeset> changesets = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
                if (changesets == null || changesets.isEmpty())
                    return null;
                result = changesets.get(0);
            } catch (IOException e) {
                Logging.warn(e);
            }
        } catch (OsmTransferException e) {
            throw e;
        } catch (IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
        return result;
    }

    /**
     * Reads the changesets with id <code>ids</code> from the server.
     *
     * @param ids the list of ids. Ignored if null. Only load changesets for ids &gt; 0.
     * @param includeDiscussion determines if discussion comments must be downloaded or not
     * @param monitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @return the changeset read
     * @throws OsmTransferException if something goes wrong
     * @throws IllegalArgumentException if id &lt;= 0
     * @since 7704
     */
    public List<Changeset> readChangesets(Collection<Integer> ids, boolean includeDiscussion, ProgressMonitor monitor)
            throws OsmTransferException {
        if (ids == null)
            return Collections.emptyList();
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(trn("Downloading {0} changeset ...", "Downloading {0} changesets ...", ids.size(), ids.size()));
            monitor.setTicksCount(ids.size());
            List<Changeset> ret = new ArrayList<>();
            int i = 0;
            for (int id : ids) {
                if (id <= 0) {
                    continue;
                }
                i++;
                try (InputStream in = getChangesetInputStream(id, includeDiscussion, monitor)) {
                    if (in == null)
                        return null;
                    monitor.indeterminateSubTask(tr("({0}/{1}) Downloading changeset {2}...", i, ids.size(), id));
                    List<Changeset> changesets = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
                    if (changesets == null || changesets.isEmpty()) {
                        continue;
                    }
                    ret.addAll(changesets);
                } catch (IOException e) {
                    Logging.warn(e);
                }
                monitor.worked(1);
            }
            return ret;
        } catch (OsmTransferException e) {
            throw e;
        } catch (IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Downloads the content of a changeset
     *
     * @param id the changeset id. &gt; 0 required.
     * @param monitor the progress monitor. {@link NullProgressMonitor#INSTANCE} assumed if null.
     * @return the changeset content
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws OsmTransferException if something went wrong
     */
    public ChangesetDataSet downloadChangeset(int id, ProgressMonitor monitor) throws OsmTransferException {
        if (id <= 0)
            throw new IllegalArgumentException(
                    MessageFormat.format("Expected value of type integer > 0 for parameter ''{0}'', got {1}", "id", id));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        ChangesetDataSet result = null;
        try {
            monitor.beginTask(tr("Downloading changeset content"));
            StringBuilder sb = new StringBuilder(32).append("changeset/").append(id).append("/download");
            try (InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true))) {
                if (in == null)
                    return null;
                monitor.setCustomText(tr("Downloading content for changeset {0} ...", id));
                OsmChangesetContentParser parser = new OsmChangesetContentParser(in);
                result = parser.parse(monitor.createSubTaskMonitor(1, true));
            } catch (IOException e) {
                Logging.warn(e);
            }
        } catch (XmlParsingException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
        return result;
    }
}
