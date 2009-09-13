// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.InputStream;
import java.util.List;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
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
    }

    /**
     * don't use - not implemented!
     *
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }


    public List<Changeset> queryChangesets(ChangesetQuery query, ProgressMonitor monitor) throws OsmTransferException {
        try {
            monitor.beginTask(tr("Reading changesetss..."));
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

    public Changeset readChangeset(long id, ProgressMonitor monitor) throws OsmTransferException {
        try {
            monitor.beginTask(tr("Reading changeset {0} ...",id));
            StringBuffer sb = new StringBuffer();
            sb.append("changeset/").append(id);
            InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            monitor.indeterminateSubTask(tr("Downloading changeset ..."));
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

    public Changeset downloadChangeset(long id, ProgressMonitor monitor) throws OsmTransferException {
        return null;
    }

}
