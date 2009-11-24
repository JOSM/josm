// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.InputStream;
import java.util.List;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Reads the history of an {@see OsmPrimitive} from the OSM API server.
 *
 */
public class OsmServerChangesetReader extends OsmServerReader {

    /**
     * constructor
     *
     */
    public OsmServerChangesetReader(){
        setDoAuthenticate(false);
    }

    /**
     * don't use - not implemented!
     *
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Queries a list
     * @param query  the query specification. Must not be null.
     * @param monitor a progress monitor. Set to {@see NullProgressMonitor#INSTANCE} if null
     * @return the list of changesets read from the server
     * @throws IllegalArgumentException thrown if query is null
     * @throws OsmTransferException
     */
    public List<Changeset> queryChangesets(ChangesetQuery query, ProgressMonitor monitor) throws OsmTransferException {
        if (query == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "query"));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Reading changesets..."));
            StringBuffer sb = new StringBuffer();
            sb.append("changesets?").append(query.getQueryString());
            InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            monitor.indeterminateSubTask(tr("Downloading changesets ..."));
            List<Changeset> changesets = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
            return changesets;
        } catch(OsmTransferException e) {
            throw e;
        } catch(IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Reads teh changeset with id <code>id</code> from the server
     *
     * @param id  the changeset id. id > 0 required.
     * @param monitor the progress monitor. Set to {@see NullProgressMonitor#INSTANCE} if null
     * @return the changeset read
     * @throws OsmTransferException thrown if something goes wrong
     * @throws IllegalArgumentException if id <= 0
     */
    public Changeset readChangeset(long id, ProgressMonitor monitor) throws OsmTransferException {
        if (id <= 0)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' > 0 expected. Got ''{1}''.", "id", id));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Reading changeset {0} ...",id));
            StringBuffer sb = new StringBuffer();
            sb.append("changeset/").append(id);
            InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            monitor.indeterminateSubTask(tr("Downloading changeset {0} ...", id));
            List<Changeset> changesets = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
            if (changesets == null || changesets.isEmpty())
                return null;
            return changesets.get(0);
        } catch(OsmTransferException e) {
            throw e;
        } catch(IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * not implemented yet
     *
     * @param id
     * @param monitor
     * @return
     * @throws OsmTransferException
     */
    public Changeset downloadChangeset(long id, ProgressMonitor monitor) throws OsmTransferException {
        return null;
    }
}
